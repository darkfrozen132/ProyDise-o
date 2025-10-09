#!/usr/bin/env python3
"""
Generador de pedidos avanzado usando pares específicos de aeropuertos
Basado en el patrón Java proporcionado
"""
import random
import csv
from datetime import datetime, timedelta
from typing import List, Tuple, Dict

class GeneradorPedidosAvanzado:
    
    def __init__(self, seed=1111):
        """Inicializar con semilla para reproducibilidad"""
        random.seed(seed)
        self.seed = seed
    
    # Pares de aeropuertos específicos (origen -> destino)
    PARES_GRUPO = [
        ("SGAS", "LOWW"),  # Asunción (SA) -> Viena (EU)
        ("SABE", "EHAM"),  # Buenos Aires (SA) -> Ámsterdam (EU)
        ("SKBO", "OMDB"),  # Bogotá (SA) -> Dubái (AS)
        ("SCEL", "LATI"),  # Santiago (SA) -> Tirana (EU)
        ("SEQM", "UBBB"),  # Quito (SA) -> Bakú (AS)
        ("SBBR", "EHAM"),  # Brasilia (SA) -> Ámsterdam (EU)
        ("SEQM", "SPIM"),  # Quito (SA) -> Lima (SA)
        ("SABE", "SGAS"),  # Buenos Aires (SA) -> Asunción (SA)
        ("EHAM", "LOWW"),  # Ámsterdam (EU) -> Viena (EU)
        ("OMDB", "UBBB"),  # Dubái (AS) -> Bakú (AS)
        
        # Pares adicionales para más variedad
        ("SVMI", "EDDI"),  # Caracas (SA) -> Berlín (EU)
        ("SBBR", "LATI"),  # Brasilia (SA) -> Tirana (EU)
        ("SKBO", "LOWW"),  # Bogotá (SA) -> Viena (EU)
        ("SCEL", "EHAM"),  # Santiago (SA) -> Ámsterdam (EU)
        ("SUAA", "LBSF"),  # Montevideo (SA) -> Sofía (EU)
        
        # Pares intra-continentales
        ("SABE", "SCEL"),  # Buenos Aires -> Santiago
        ("SKBO", "SEQM"),  # Bogotá -> Quito
        ("EHAM", "EDDI"),  # Ámsterdam -> Berlín
        ("LOWW", "LATI"),  # Viena -> Tirana
        ("OMDB", "OOMS"),  # Dubái -> Muscat
    ]
    
    # Información de continentes para calcular plazos
    AEROPUERTOS_INFO = {
        # Sudamérica
        "SGAS": {"continente": "SA", "nombre": "Asunción"},
        "SABE": {"continente": "SA", "nombre": "Buenos Aires"},
        "SKBO": {"continente": "SA", "nombre": "Bogotá"},
        "SCEL": {"continente": "SA", "nombre": "Santiago"},
        "SEQM": {"continente": "SA", "nombre": "Quito"},
        "SBBR": {"continente": "SA", "nombre": "Brasilia"},
        "SPIM": {"continente": "SA", "nombre": "Lima"},
        "SVMI": {"continente": "SA", "nombre": "Caracas"},
        "SUAA": {"continente": "SA", "nombre": "Montevideo"},
        
        # Europa
        "LOWW": {"continente": "EU", "nombre": "Viena"},
        "EHAM": {"continente": "EU", "nombre": "Ámsterdam"},
        "LATI": {"continente": "EU", "nombre": "Tirana"},
        "EDDI": {"continente": "EU", "nombre": "Berlín"},
        "LBSF": {"continente": "EU", "nombre": "Sofía"},
        "EBCI": {"continente": "EU", "nombre": "Bruselas"},
        
        # Asia
        "OMDB": {"continente": "AS", "nombre": "Dubái"},
        "UBBB": {"continente": "AS", "nombre": "Bakú"},
        "OOMS": {"continente": "AS", "nombre": "Muscat"},
    }
    
    # Plazos de entrega (en minutos)
    PLAZO_INTRA = 24 * 60    # 24 horas para mismo continente
    PLAZO_INTER = 48 * 60    # 48 horas para diferente continente
    
    # IDs de clientes
    CLIENTES = ["0001111", "0002222", "0003333", "0004444", "0005555", 
               "0006666", "0007777", "0008888", "0009999", "0001234"]
    
    def mismo_continente(self, origen: str, destino: str) -> bool:
        """Verifica si dos aeropuertos están en el mismo continente"""
        if origen not in self.AEROPUERTOS_INFO or destino not in self.AEROPUERTOS_INFO:
            return False
        return self.AEROPUERTOS_INFO[origen]["continente"] == self.AEROPUERTOS_INFO[destino]["continente"]
    
    def construir_grupo_pedidos(self, fecha_ancla: datetime, pares: List[Tuple[str, str]], start_id: int = 1) -> List[Dict]:
        """Construir grupo de pedidos basado en pares de aeropuertos"""
        pedidos = []
        pedido_id = start_id
        
        for origen, destino in pares:
            # Verificar que ambos aeropuertos existen
            if origen not in self.AEROPUERTOS_INFO or destino not in self.AEROPUERTOS_INFO:
                print(f"[WARN] Pedido ignorado: {origen} -> {destino} (aeropuerto no encontrado)")
                continue
            
            # Generar datos del pedido
            cantidad = 80 + random.randint(0, 100)  # Entre 80 y 180 paquetes
            
            # Hora de liberación: entre 6:00 y 12:00
            hora_liberacion = 6 + random.randint(0, 6)
            minuto_liberacion = random.randint(0, 59)
            
            # Calcular plazo según si es mismo continente
            mismo_cont = self.mismo_continente(origen, destino)
            plazo_minutos = self.PLAZO_INTRA if mismo_cont else self.PLAZO_INTER
            
            # Cliente aleatorio
            cliente = random.choice(self.CLIENTES)
            
            pedido = {
                "id": pedido_id,
                "dia": fecha_ancla.day,
                "hora": hora_liberacion,
                "minuto": minuto_liberacion,
                "origen": origen,
                "destino": destino,
                "cantidad": cantidad,
                "cliente": cliente,
                "mismo_continente": mismo_cont,
                "plazo_minutos": plazo_minutos
            }
            
            pedidos.append(pedido)
            pedido_id += 1
        
        return pedidos
    
    def generar_pedidos_csv(self, nombre_archivo: str, num_pedidos: int = 20, fecha_base: datetime = None):
        """Generar archivo CSV con pedidos usando pares específicos"""
        if fecha_base is None:
            fecha_base = datetime(2024, 9, 5)  # 5 de septiembre 2024
        
        print(f"🎲 Generando pedidos con semilla: {self.seed}")
        print(f"📅 Fecha base: {fecha_base.strftime('%Y-%m-%d')}")
        print(f"📋 Pedidos objetivo: {num_pedidos}")
        
        # Seleccionar pares aleatorios
        pares_seleccionados = random.sample(self.PARES_GRUPO, min(num_pedidos, len(self.PARES_GRUPO)))
        
        # Si necesitamos más pedidos, duplicar algunos pares con variación
        while len(pares_seleccionados) < num_pedidos:
            par_extra = random.choice(self.PARES_GRUPO)
            pares_seleccionados.append(par_extra)
        
        # Limitar al número exacto solicitado
        pares_seleccionados = pares_seleccionados[:num_pedidos]
        
        # Generar pedidos
        pedidos = self.construir_grupo_pedidos(fecha_base, pares_seleccionados)
        
        # Escribir CSV
        with open(nombre_archivo, 'w', newline='', encoding='utf-8') as csvfile:
            csvfile.write("IdPedido\n")
            
            for pedido in pedidos:
                # Formato: dd-hh-mm-dest-###-IdClien
                pedido_str = f"{pedido['dia']:02d}-{pedido['hora']:02d}-{pedido['minuto']:02d}-{pedido['destino']}-{pedido['cantidad']:03d}-{pedido['cliente']}"
                csvfile.write(f"{pedido_str}\n")
        
        # Mostrar estadísticas
        print(f"\n✅ Archivo generado: {nombre_archivo}")
        print(f"📊 Pedidos generados: {len(pedidos)}")
        
        # Estadísticas por continente
        intra = sum(1 for p in pedidos if p["mismo_continente"])
        inter = len(pedidos) - intra
        print(f"🌎 Distribución: {intra} intra-continentales, {inter} inter-continentales")
        
        # Mostrar algunos ejemplos
        print(f"\n📋 Ejemplos de pedidos generados:")
        for i, pedido in enumerate(pedidos[:5]):
            origen_info = self.AEROPUERTOS_INFO[pedido["origen"]]
            destino_info = self.AEROPUERTOS_INFO[pedido["destino"]]
            tipo = "INTRA" if pedido["mismo_continente"] else "INTER"
            
            print(f"  [{i+1}] {origen_info['nombre']} -> {destino_info['nombre']} "
                  f"({pedido['cantidad']} paq, {tipo})")
        
        if len(pedidos) > 5:
            print(f"  ... y {len(pedidos) - 5} más")
        
        return pedidos

def main():
    """Función principal para generar diferentes tipos de archivos"""
    print("🚀 GENERADOR AVANZADO DE PEDIDOS - MoraPack")
    print("=" * 50)
    
    # Crear directorio si no existe
    import os
    os.makedirs("pedidos", exist_ok=True)
    
    # Generar diferentes archivos con diferentes semillas
    configuraciones = [
        {"archivo": "pedidos/pedidos_pares_01.csv", "pedidos": 20, "semilla": 1111},
        {"archivo": "pedidos/pedidos_pares_02.csv", "pedidos": 15, "semilla": 2222},
        {"archivo": "pedidos/pedidos_pares_03.csv", "pedidos": 25, "semilla": 3333},
        {"archivo": "pedidos/pedidos_test_pares.csv", "pedidos": 10, "semilla": 9999}
    ]
    
    for config in configuraciones:
        print(f"\n{'='*60}")
        generador = GeneradorPedidosAvanzado(seed=config["semilla"])
        generador.generar_pedidos_csv(
            config["archivo"], 
            config["pedidos"]
        )
    
    print(f"\n🎉 ¡Todos los archivos generados exitosamente!")

if __name__ == "__main__":
    main()
