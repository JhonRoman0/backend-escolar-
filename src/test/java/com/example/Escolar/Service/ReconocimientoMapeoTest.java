package com.example.Escolar.Service;

import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/** Valida entidades y JPQL sin conexion JDBC ni generacion de esquema. */
class ReconocimientoMapeoTest {
    @Test void mapeoYConsultaDeAsignacionesSonValidos() throws ReflectiveOperationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl");
        var registry = new org.hibernate.boot.registry.StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        var sources = new org.hibernate.boot.MetadataSources(registry);
        for (var bean : scanner.findCandidateComponents("com.example.Escolar.Model")) sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
        var metadata = sources.buildMetadata();
        var tabla = metadata.getEntityBinding("com.example.Escolar.Model.ReconocimientoCalificacion").getTable();
        var checks = tabla.getChecks().stream().collect(java.util.stream.Collectors.toMap(
            org.hibernate.mapping.CheckConstraint::getName, org.hibernate.mapping.CheckConstraint::getConstraint));
        org.junit.jupiter.api.Assertions.assertEquals(2, checks.size());
        org.junit.jupiter.api.Assertions.assertEquals("id_nota_origen <> id_nota_destino", checks.get("ck_reconocimiento_distinto"));
        org.junit.jupiter.api.Assertions.assertTrue(checks.get("ck_reconocimiento_estado").contains("char_length(trim(motivo_anulacion)) > 0"));
        try (var factory = metadata.buildSessionFactory(); var session = factory.openSession()) {
            for (var repository : java.util.List.of(com.example.Escolar.Repository.ReconocimientoCalificacionRepository.class, com.example.Escolar.Repository.NotaEvaluacionRepository.class)) {
                for (var method : repository.getDeclaredMethods()) {
                    var query = method.getAnnotation(org.springframework.data.jpa.repository.Query.class);
                    if (query != null && !query.nativeQuery()) session.createQuery(query.value());
                }
            }
        } finally { org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry); }
    }
}
