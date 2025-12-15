#!/usr/bin/env python3
"""
Parche para agregar sistema de buffer de 15 segundos a SimuladorSemanal.js
"""

import re

# Leer el archivo
with open('SimuladorSemanal.js', 'r', encoding='utf-8') as f:
    content = f.read()

# Código viejo a buscar (dentro de procesarVuelosDirectos, después de ELIMINAR DUPLICADOS)
old_code = '''			// 🔥 REEMPLAZAR todos los vuelos (sin duplicados)
			console.log(`🔄 Reemplazando flights array con ${vuelosUnicos.length} vuelos únicos`);
			setFlights(vuelosUnicos);
			
			// 🆕 LOG: Mostrar rango de fechas de los vuelos
			const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
			if (vuelosConFecha.length > 0) {
				const fechaMasTemprana = Math.min(...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime()));
				const fechaMasTardia = Math.max(...vuelosConFecha.map(v => new Date(v.fechaFinal).getTime()));
				console.log(`'''

# Buscar todas las ocurrencias del patrón inicial
occurrences = list(re.finditer(re.escape(old_code[:100]), content))
print(f"Encontradas {len(occurrences)} ocurrencias del patrón inicial")

# Necesitamos encontrar la ocurrencia correcta (dentro de procesarVuelosDirectos)
# Buscar la función procesarVuelosDirectos
procesarVD_match = re.search(r'const procesarVuelosDirectos = useCallback\(\(vuelos\) => \{', content)
if procesarVD_match:
    print(f"procesarVuelosDirectos encontrado en posición {procesarVD_match.start()}")
    
    # Buscar la ocurrencia del patrón que está después de procesarVuelosDirectos
    for occ in occurrences:
        if occ.start() > procesarVD_match.start():
            print(f"Ocurrencia relevante encontrada en posición {occ.start()}")
            
            # Encontrar el final del bloque a reemplazar
            # Buscar desde esta posición hasta "console.log(`✈️ Vuelos activos:"
            search_start = occ.start()
            search_end = content.find("console.log(`✈️ Vuelos activos:", search_start)
            
            if search_end > search_start:
                # Encontrar el final de la línea
                line_end = content.find('\n', search_end)
                block_end = line_end + 1
                
                old_block = content[search_start:block_end]
                print(f"Bloque a reemplazar: {len(old_block)} caracteres")
                print(f"Primeros 200 chars: {old_block[:200]}")
                print(f"Últimos 200 chars: {old_block[-200:]}")
                
                # Nuevo código
                new_code = '''			// 🆕 LOG: Mostrar rango de fechas de los vuelos
			const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
			if (vuelosConFecha.length > 0) {
				const fechaMasTemprana = Math.min(...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime()));
				const fechaMasTardia = Math.max(...vuelosConFecha.map(v => new Date(v.fechaFinal).getTime()));
				console.log(`📆 Rango de vuelos: ${new Date(fechaMasTemprana).toISOString()} → ${new Date(fechaMasTardia).toISOString()}`);
				console.log(`⏰ Tiempo simulado actual: ${new Date(relojLocalRef.current).toISOString()}`);
			}
			
			// 🆕 SISTEMA DE BUFFER DE 15 SEGUNDOS
			// Si el buffer está activo, acumular vuelos en lugar de activar animación
			if (bufferActivo) {
				console.log(`⏳ BUFFER ACTIVO - Acumulando ${vuelosUnicos.length} vuelos (total: ${vuelosBufferRef.current.length + vuelosUnicos.length})`);
				
				// Agregar vuelos únicos al buffer (evitar duplicados)
				const idsExistentes = new Set(vuelosBufferRef.current.map(v => v.id));
				const nuevosParaBuffer = vuelosUnicos.filter(v => !idsExistentes.has(v.id));
				vuelosBufferRef.current = [...vuelosBufferRef.current, ...nuevosParaBuffer];
				
				console.log(`📦 Buffer ahora tiene ${vuelosBufferRef.current.length} vuelos`);
				// NO activar reloj durante el buffer - se activa cuando termina el timeout
				return;
			}
			
			// 🔥 COMBINAR con vuelos existentes (sin duplicados) - Solo si buffer NO está activo
			console.log(`🔄 Combinando ${vuelosUnicos.length} vuelos nuevos con existentes`);
			
			setFlights(prevFlights => {
				const existingIds = new Set(prevFlights.map(v => v.id));
				const nuevosNoRepetidos = vuelosUnicos.filter(v => !existingIds.has(v.id));
				const combinados = [...prevFlights, ...nuevosNoRepetidos];
				console.log(`📊 Total vuelos después de combinar: ${combinados.length}`);
				return combinados;
			});
			
			// 🚀 ACTIVAR RELOJ LOCAL (solo si no está ya activo)
			if (!simulacionLocalActiva) {
				setSimulacionLocalActiva(true);
				console.log(`🚀 Reloj local ACTIVADO - ${vuelosUnicos.length} vuelos listos para animar`);
			}
			
			// ✅ Verificar que el estado se actualizó
			setTimeout(() => {
				console.log(`✅ Verificación: nuevos vuelos procesados = ${vuelosUnicos.length}`);
			}, 100);
			
			const vuelosActivos = vuelosUnicos.filter(v => v.status === 'active').length;
			setFlightsInAir(prev => prev + vuelosActivos);
			console.log(`✈️ Vuelos activos añadidos: ${vuelosActivos}`);
'''
                
                # Hacer el reemplazo
                new_content = content[:search_start] + new_code + content[block_end:]
                
                # Guardar
                with open('SimuladorSemanal.js', 'w', encoding='utf-8') as f:
                    f.write(new_content)
                
                print("✅ Parche aplicado exitosamente")
                break
            else:
                print("ERROR: No se encontró el final del bloque")
            break
else:
    print("ERROR: No se encontró procesarVuelosDirectos")
