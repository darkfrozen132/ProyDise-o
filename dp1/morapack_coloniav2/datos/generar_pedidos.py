import random
import os

# Aeropuertos destino disponibles - TODOS los del CSV aeropuertos_simple.csv
aeropuertos_destino = [
    # América del Sur
    'SKBO',  # Bogota, Colombia
    'SEQM',  # Quito, Ecuador  
    'SVMI',  # Caracas, Venezuela
    'SBBR',  # Brasilia, Brasil
    'SPIM',  # Lima, Peru (SEDE)
    'SLLP',  # La_Paz, Bolivia
    'SCEL',  # Santiago_de_Chile, Chile
    'SABE',  # Buenos_Aires, Argentina
    'SGAS',  # Asuncion, Paraguay
    'SUAA',  # Montevideo, Uruguay
    
    # Europa
    'LATI',  # Tirana, Albania
    'EDDI',  # Berlin, Alemania
    'LOWW',  # Viena, Austria
    'EBCI',  # Bruselas, Belgica (SEDE)
    'UMMS',  # Minsk, Bielorrusia
    'LBSF',  # Sofia, Bulgaria
    'LKPR',  # Praga, Republica_Checa
    'LDZA',  # Zagreb, Croacia
    'EKCH',  # Copenhague, Dinamarca
    'EHAM',  # Amsterdam, Paises_Bajos
    
    # Asia y Medio Oriente
    'VIDP',  # Delhi, India
    'OSDI',  # Damasco, Siria
    'OERK',  # Riad, Arabia_Saudita
    'OMDB',  # Dubai, Emiratos_Arabes_Unidos
    'OAKB',  # Kabul, Afganistan
    'OOMS',  # Mascate, Oman
    'OYSN',  # Sana, Yemen
    'OPKC',  # Karachi, Pakistan
    'UBBB',  # Baku, Azerbaiyan (SEDE)
    'OJAI',  # Aman, Jordania
    'LTBA',  # Estambul, Turquia
    'UUDD',  # Moscu, Rusia
    'ZBAA'   # Pekin, China
]

# Sedes MoraPack (no incluir como destinos frecuentes)
sedes_morapack = ['SPIM', 'EBCI', 'UBBB']

# Aeropuertos destino sin las sedes (para generar más pedidos internacionales)
aeropuertos_destino_sin_sedes = [aeropuerto for aeropuerto in aeropuertos_destino 
                                if aeropuerto not in sedes_morapack]

# Clientes disponibles
clientes = ['0000001', '0000123', '0001234', '0005678', '0009876', '0012345', '0054321', '0098765']

def generar_archivo_pedidos(numero_archivo):
    nombre_archivo = f"pedidos_{numero_archivo:02d}.csv"
    ruta_archivo = f"pedidos/{nombre_archivo}"
    
    with open(ruta_archivo, 'w') as f:
        f.write("IdPedido\n")
        
        # Generar 100 pedidos aleatorios con distribución realista
        for i in range(20):
            dia = random.randint(1, 31)
            hora = random.randint(7, 17)  # Horario de trabajo
            minuto = random.choice([0, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55])
            
            # 80% de pedidos a destinos internacionales (sin sedes)
            # 20% de pedidos que pueden incluir sedes (transferencias internas)
            if random.random() < 0.8:
                destino = random.choice(aeropuertos_destino_sin_sedes)
            else:
                destino = random.choice(aeropuertos_destino)
            
            cantidad = random.randint(0, 200)  # Cantidad entre 45 y 500
            cliente = random.choice(clientes)
            
            # Formato: dd-hh-mm-dest-###-IdClien
            pedido_id = f"{dia:02d}-{hora:02d}-{minuto:02d}-{destino}-{cantidad:03d}-{cliente}"
            f.write(f"{pedido_id}\n")
    
    print(f"Generado: {nombre_archivo} con aeropuertos del CSV completo")

def generar_archivo_pedidos_especifico(numero_archivo, aeropuertos_objetivo):
    """Genera archivo con aeropuertos específicos para testing"""
    nombre_archivo = f"pedidos_test_{numero_archivo:02d}.csv"
    ruta_archivo = f"pedidos/{nombre_archivo}"
    
    with open(ruta_archivo, 'w') as f:
        f.write("IdPedido\n")
        
        # Generar pedidos específicos para aeropuertos objetivo
        for i, destino in enumerate(aeropuertos_objetivo):
            dia = random.randint(1, 31)
            hora = random.randint(8, 16)
            minuto = random.choice([0, 15, 30, 45])
            cantidad = random.randint(0, 200)
            cliente = random.choice(clientes)
            
            pedido_id = f"{dia:02d}-{hora:02d}-{minuto:02d}-{destino}-{cantidad:03d}-{cliente}"
            f.write(f"{pedido_id}\n")
    
    print(f"Generado: {nombre_archivo} para testing específico")

def mostrar_estadisticas():
    """Muestra estadísticas de los aeropuertos disponibles"""
    print("\n📊 ESTADÍSTICAS DE AEROPUERTOS DISPONIBLES:")
    print("==========================================")
    print(f"🌍 Total aeropuertos: {len(aeropuertos_destino)}")
    print(f"🏢 Sedes MoraPack: {len(sedes_morapack)} - {sedes_morapack}")
    print(f"✈️ Destinos internacionales: {len(aeropuertos_destino_sin_sedes)}")
    
    print("\n🌎 DISTRIBUCIÓN POR REGIÓN:")
    print("---------------------------")
    print("🇺🇸 América del Sur: SKBO, SEQM, SVMI, SBBR, SPIM*, SLLP, SCEL, SABE, SGAS, SUAA")
    print("🇪🇺 Europa: LATI, EDDI, LOWW, EBCI*, UMMS, LBSF, LKPR, LDZA, EKCH, EHAM") 
    print("🌏 Asia/Medio Oriente: VIDP, OSDI, OERK, OMDB, OAKB, OOMS, OYSN, OPKC, UBBB*, OJAI, LTBA, UUDD, ZBAA")
    print("\n* = Sede MoraPack")
    
    print(f"\n📈 CONFIGURACIÓN DE GENERACIÓN:")
    print("------------------------------")
    print("• 80% pedidos a destinos internacionales")
    print("• 20% pedidos que pueden incluir sedes")
    print("• 100 pedidos por archivo")
    print("• Horarios: 07:00-17:00 (horario laboral)")
    print("• Cantidades: 45-500 unidades")
    print(f"• Clientes: {len(clientes)} diferentes")
    
# Función para generar archivos de testing específicos
def generar_archivos_testing():
    print("\n🧪 GENERANDO ARCHIVOS DE TESTING:")
    print("=================================")
    
    # Test 1: Solo destinos europeos
    destinos_europa = ['LATI', 'EDDI', 'LOWW', 'UMMS', 'LBSF', 'LKPR', 'LDZA', 'EKCH', 'EHAM']
    generar_archivo_pedidos_especifico(1, destinos_europa[:8])
    
    # Test 2: Solo destinos asiáticos/medio oriente  
    destinos_asia = ['VIDP', 'OSDI', 'OERK', 'OMDB', 'OAKB', 'OOMS', 'OYSN', 'OPKC']
    generar_archivo_pedidos_especifico(2, destinos_asia)
    
    # Test 3: Solo destinos sudamericanos (sin sedes)
    destinos_sudamerica = ['SKBO', 'SEQM', 'SVMI', 'SBBR', 'SLLP', 'SCEL', 'SABE', 'SGAS']
    generar_archivo_pedidos_especifico(3, destinos_sudamerica)

# Crear directorio si no existe
os.makedirs("pedidos", exist_ok=True)

# Mostrar estadísticas de aeropuertos
mostrar_estadisticas()

print("\n🚀 INICIANDO GENERACIÓN DE PEDIDOS:")
print("===================================")

# Generar los 20 archivos principales
for i in range(1, 21):
    generar_archivo_pedidos(i)

# Generar archivos de testing específicos
generar_archivos_testing()

print("\n✅ ¡GENERACIÓN COMPLETADA!")
print("========================")
print("📁 20 archivos principales generados: pedidos_01.csv a pedidos_20.csv")
print("🧪 3 archivos de testing generados: pedidos_test_01.csv a pedidos_test_03.csv")
print(f"🌍 Usando {len(aeropuertos_destino)} aeropuertos del CSV completo")
print("🏢 Sedes MoraPack incluidas: SPIM (Lima), EBCI (Bruselas), UBBB (Baku)")
print("\n🎯 Los pedidos están listos para el algoritmo genético integrado!")
