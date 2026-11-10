package com.obratech.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;
import com.obratech.entity.Postulacion;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoPostulacion;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

@Service
public class ProjectFitSimulationService {

    private static final double TECHNICAL_WEIGHT = 0.50;
    private static final double BUDGET_WEIGHT = 0.25;
    private static final double LOCATION_WEIGHT = 0.25;

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final PerfilRepository perfilRepository;

    public ProjectFitSimulationService(
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            PerfilRepository perfilRepository) {
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.perfilRepository = perfilRepository;
    }

    public Simulation simulate(Proyecto project, Perfil contractor) {
        double fitTecnico = technicalFit(project, contractor);
        double fitPresupuesto = budgetFit(project, contractor);
        double fitUbicacion = locationFit(project, contractor);
        double compatibility = clamp(
                fitTecnico * TECHNICAL_WEIGHT
                        + fitPresupuesto * BUDGET_WEIGHT
                        + fitUbicacion * LOCATION_WEIGHT);

        List<Postulacion> activeApplications = postulacionRepository.findByProyectoId(project.getId()).stream()
                .filter(application -> application.getEstado() == null
                        || application.getEstado() == EstadoPostulacion.PENDING
                        || application.getEstado() == EstadoPostulacion.OFFERED)
                .toList();
        List<Double> competitorFits = activeApplications.stream()
                .map(Postulacion::getUsuario)
                .filter(java.util.Objects::nonNull)
                .map(this::findContractorProfile)
                .filter(java.util.Objects::nonNull)
                .map(profile -> overallFit(project, profile))
                .toList();
        double competitionAverage = competitorFits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        long strongerCompetitors = competitorFits.stream()
                .filter(score -> score > compatibility)
                .count();
        double competitionFactor = activeApplications.isEmpty()
                ? 1.0
                : clamp(1.0 / (1.0 + strongerCompetitors * 0.35 + activeApplications.size() * 0.04));
        double timeFactor = timeFactor(project);
        double winProbability = clamp(compatibility * 0.70 + competitionFactor * 0.20 + timeFactor * 0.10);

        return new Simulation(
                round(fitTecnico),
                round(fitPresupuesto),
                round(fitUbicacion),
                round(compatibility),
                round(winProbability),
                activeApplications.size(),
                round(competitionAverage),
                strongerCompetitors,
                round(timeFactor));
    }

    private Perfil findContractorProfile(Usuario user) {
        return user.getUsername() == null ? null
                : perfilRepository.findByUsernameIgnoreCase(user.getUsername()).orElse(null);
    }

    private double overallFit(Proyecto project, Perfil contractor) {
        return clamp(technicalFit(project, contractor) * TECHNICAL_WEIGHT
                + budgetFit(project, contractor) * BUDGET_WEIGHT
                + locationFit(project, contractor) * LOCATION_WEIGHT);
    }

    private double technicalFit(Proyecto project, Perfil contractor) {
        String projectType = normalize(project.getTipoProyecto());
        String specialty = normalize(contractor.getEspecialidad());
        Set<String> subSpecialties = contractor.getSubespecialidades() == null
                ? Set.of() : contractor.getSubespecialidades();
        String allSpecialties = normalize(specialty + " " + String.join(" ", subSpecialties));
        double experience = clamp((contractor.getExperiencia() == null ? 0 : contractor.getExperiencia()) / 10.0);
        if (projectType.isBlank() || allSpecialties.isBlank()) {
            return clamp(0.25 + experience * 0.50);
        }
        boolean categoryMatch = allSpecialties.contains(projectType)
                || projectType.contains(allSpecialties)
                || projectType.split(" ").length > 0
                        && java.util.Arrays.stream(projectType.split(" "))
                                .anyMatch(token -> token.length() > 3 && allSpecialties.contains(token));
        return clamp((categoryMatch ? 0.70 : 0.35) + experience * 0.30);
    }

    private double budgetFit(Proyecto project, Perfil contractor) {
        if (project.getPresupuesto() == null || contractor.getId() == null) {
            return 0.50;
        }
        List<Double> historicalBudgets = proyectoRepository.findByContratistaAsignadoId(contractor.getId()).stream()
                .map(Proyecto::getPresupuesto)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (historicalBudgets.isEmpty()) {
            return 0.50;
        }
        double average = historicalBudgets.stream().mapToDouble(Double::doubleValue).average().orElse(project.getPresupuesto());
        double ratio = Math.abs(Math.log((project.getPresupuesto() + 1.0) / (average + 1.0)));
        return clamp(Math.exp(-ratio));
    }

    private double locationFit(Proyecto project, Perfil contractor) {
        String projectDepartment = project.getUbicacionGeografica() == null ? "" : project.getUbicacionGeografica().getDepartamento();
        String projectMunicipality = project.getUbicacionGeografica() == null ? "" : project.getUbicacionGeografica().getMunicipio();
        String contractorDepartment = contractor.getDepartamento();
        String contractorCity = contractor.getCiudad();
        if (same(projectMunicipality, contractorCity)) {
            return 1.0;
        }
        if (same(projectDepartment, contractorDepartment)) {
            return 0.75;
        }
        if (same(project.getUbicacion(), contractor.getUbicacion())) {
            return 0.60;
        }
        return 0.30;
    }

    private double timeFactor(Proyecto project) {
        if (project.getFechaLimitePostulacion() == null) {
            return 0.60;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), project.getFechaLimitePostulacion());
        return days <= 1 ? 0.90 : days <= 3 ? 0.75 : 0.55;
    }

    private boolean same(String left, String right) {
        return !normalize(left).isBlank() && normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public record Simulation(
            double fitTecnico,
            double fitPresupuesto,
            double fitUbicacion,
            double compatibilidadGeneral,
            double probabilidadGanar,
            int postulantesActivos,
            double fitPromedioCompetencia,
            long competidoresMasFuertes,
            double factorTiempo) {
    }
}
