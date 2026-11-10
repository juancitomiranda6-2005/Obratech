package com.obratech.service;

import org.springframework.stereotype.Service;

import com.obratech.client.MlServiceClient;
import com.obratech.dto.ml.PrediccionRetrasoRequest;
import com.obratech.dto.ml.PrediccionRetrasoResponse;
import com.obratech.dto.ml.PrediccionSeleccionRequest;
import com.obratech.dto.ml.PrediccionSeleccionResponse;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;

@Service
public class PrediccionService {

    private final MlServiceClient mlServiceClient;
    private final PrediccionMapper prediccionMapper;

    public PrediccionService(MlServiceClient mlServiceClient, PrediccionMapper prediccionMapper) {
        this.mlServiceClient = mlServiceClient;
        this.prediccionMapper = prediccionMapper;
    }

    public PrediccionRetrasoResponse predecirRetraso(PrediccionRetrasoRequest request) {
        // TODO: Validar permisos y enriquecer la solicitud con datos de la obra.
        return mlServiceClient.predecirRetraso(request);
    }

    public PrediccionSeleccionResponse predecirSeleccion(PrediccionSeleccionRequest request) {
        // TODO: Validar la postulación y enriquecerla con datos del contratista.
        return mlServiceClient.predecirSeleccion(request);
    }

    public PrediccionRetrasoResponse predecirRetraso(Proyecto proyecto, String perfilId) {
        return predecirRetraso(prediccionMapper.construirRetraso(proyecto, perfilId));
    }

    public PrediccionSeleccionResponse predecirSeleccion(Proyecto proyecto, Perfil contratista) {
        return predecirSeleccion(prediccionMapper.construirSeleccion(proyecto, contratista));
    }
}
