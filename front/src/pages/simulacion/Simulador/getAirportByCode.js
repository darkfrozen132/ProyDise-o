/* ======= Obtener aeropuerto por código ======= */
export const getAirportByCode = (code, airports = []) => {
	if (!code || !Array.isArray(airports)) return null;
	return airports.find(a => String(a.code || '').toUpperCase() === String(code || '').toUpperCase()) || null;
};
