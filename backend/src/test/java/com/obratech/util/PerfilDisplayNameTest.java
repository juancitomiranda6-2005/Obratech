package com.obratech.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.obratech.entity.Perfil;

class PerfilDisplayNameTest {

    @Test
    void joinsTrimmedNameAndSurname() {
        Perfil perfil = new Perfil();
        perfil.setNombre(" Ana ");
        perfil.setApellido(" Pérez ");

        assertEquals("Ana Pérez", PerfilDisplayName.of(perfil));
    }

    @Test
    void fallsBackToUsernameWhenNameIsMissing() {
        Perfil perfil = new Perfil();
        perfil.setUsername("ana@example.test");

        assertEquals("ana@example.test", PerfilDisplayName.of(perfil));
    }
}