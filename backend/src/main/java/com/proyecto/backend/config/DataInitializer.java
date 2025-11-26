package com.proyecto.backend.config;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.repository.AeropuertoRepository;
import com.proyecto.backend.repository.PedidoRepository;
import com.proyecto.backend.repository.PlanDeVueloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AeropuertoRepository aeropuertoRepository;
    private final PedidoRepository pedidoRepository;
    private final PlanDeVueloRepository planDeVueloRepository;

    @Override
    public void run(String... args) {
        if (aeropuertoRepository.count() == 0) {
            loadAeropuertos();
        }
        if (planDeVueloRepository.count() == 0) {
            loadPlanesDeVuelo();
        }
        if (pedidoRepository.count() == 0) {
            loadPedidos();
        }
        log.info("✅ Inicialización completada: {} aeropuertos, {} vuelos, {} pedidos",
                aeropuertoRepository.count(),
                planDeVueloRepository.count(),
                pedidoRepository.count());
    }

    private void loadAeropuertos() {
        try {
            ClassPathResource resource = new ClassPathResource("datos/Aeropuertos.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 8) {
                    Aeropuerto aeropuerto = new Aeropuerto();
                    aeropuerto.setCodigoICAO(parts[0].trim());
                    aeropuerto.setCiudad(parts[1].trim());
                    aeropuerto.setPais(parts[2].trim());
                    aeropuerto.setHusoHorario(Integer.parseInt(parts[3].trim()));
                    aeropuerto.setCapacidadAlmacen(Integer.parseInt(parts[4].trim()));
                    aeropuerto.setLatitud(Double.parseDouble(parts[5].trim()));
                    aeropuerto.setLongitud(Double.parseDouble(parts[6].trim()));
                    aeropuerto.setContinente(parts[7].trim());
                    aeropuertoRepository.save(aeropuerto);
                    count++;
                }
            }
            reader.close();
            log.info("✅ Cargados {} aeropuertos", count);
        } catch (Exception e) {
            log.error("❌ Error cargando aeropuertos: {}", e.getMessage());
        }
    }

    private void loadPlanesDeVuelo() {
        try {
            ClassPathResource resource = new ClassPathResource("datos/PlanesDeVuelo.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    PlanDeVuelo vuelo = new PlanDeVuelo();
                    vuelo.setAeropuertoOrigen(parts[0].trim());
                    vuelo.setAeropuertoDestino(parts[1].trim());
                    vuelo.setHoraSalida(LocalTime.parse(parts[2].trim(), DateTimeFormatter.ofPattern("HH:mm")));
                    vuelo.setHoraLlegada(LocalTime.parse(parts[3].trim(), DateTimeFormatter.ofPattern("HH:mm")));
                    vuelo.setCapacidadMaxima(Integer.parseInt(parts[4].trim()));
                    planDeVueloRepository.save(vuelo);
                    count++;
                }
            }
            reader.close();
            log.info("✅ Cargados {} planes de vuelo", count);
        } catch (Exception e) {
            log.error("❌ Error cargando planes de vuelo: {}", e.getMessage());
        }
    }

    private void loadPedidos() {
        try {
            ClassPathResource resource = new ClassPathResource("datos/Pedidos.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("-");
                if (parts.length >= 6) {
                    Pedido pedido = new Pedido();
                    pedido.setDia(Integer.parseInt(parts[0].trim()));
                    pedido.setHora(Integer.parseInt(parts[1].trim()));
                    pedido.setMinuto(Integer.parseInt(parts[2].trim()));
                    pedido.setAeropuertoDestinoId(parts[3].trim());
                    pedido.setCantidadProductos(Integer.parseInt(parts[4].trim()));
                    pedido.setClienteId(parts[5].trim());
                    pedido.setAnio(2025);
                    pedido.setMes(1);
                    pedido.setEstado("PENDIENTE");
                    pedidoRepository.save(pedido);
                    count++;
                }
            }
            reader.close();
            log.info("✅ Cargados {} pedidos", count);
        } catch (Exception e) {
            log.error("❌ Error cargando pedidos: {}", e.getMessage());
        }
    }
}
