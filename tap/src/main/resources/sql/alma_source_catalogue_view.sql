CREATE OR REPLACE FORCE VIEW source_cone_search (
    catalogue_name,
    source_names,
    m_ra,
    m_ra_uncertainty,
	m_dec,
    m_dec_uncertainty,
	m_frequency, 
	m_flux, 
	m_flux_uncertainty, 
	m_origin,
	m_degree, 
	m_degree_uncertainty, 
	m_angle, 
	m_angle_uncertainty, 
	m_date_observed, 
	m_frequency_support_display,
	s_ra_deg,
	s_ra_deg_uncertainty, 
	s_dec_deg, 
	s_dec_deg_uncertainty,
	s_center,
	b_spectral_index,
	band_name
 ) AS SELECT 
    c.catalogue_name,
	subquery_sourcenames.SOURCE_NAMES,
	m.ra, 
	m.ra_uncertainty, 
	m.dec, 
	m.dec_uncertainty, 
	m.frequency, 
	m.flux, 
	m.flux_uncertainty, 
	m.origin,
	m.degree, 
	m.degree_uncertainty, 
	m.angle, 
	m.angle_uncertainty,
	m.date_observed, 
	m.frequency_support_display,
	s.ra_deg,
	s.ra_deg_uncertainty,
	s.dec_deg, 
	s.dec_deg_uncertainty,
	s.center,
	b.spectral_index,
	coalesce(r.range_name, 'non-ALMA Band')
FROM measurements m 
INNER JOIN sources s on s.source_id = m.source_id 
INNER JOIN catalogues c on c.catalogue_id = m.catalogue_id
LEFT JOIN ranges r ON m.frequency BETWEEN r.frequency_min AND r.frequency_max 
LEFT JOIN source_band b ON b.band_id = r.range_id AND b.source_id = s.source_id 
LEFT JOIN ( SELECT snn.source_id, LISTAGG(name, ', ') SOURCE_NAMES FROM sourcename snn GROUP BY snn.source_id ) subquery_sourcenames ON subquery_sourcenames.source_id = s.source_id 
INNER JOIN ( SELECT source_id FROM source_type WHERE (type_id = 1 ) GROUP BY source_id ) subquery_type ON subquery_type.source_id = s.source_id 
WHERE (1 = 1) AND valid = 1;
