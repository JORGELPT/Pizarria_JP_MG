-- ============================================================
-- Agrega la columna unidad_medida a tbl_ingrediente
-- Ejecutar en SQL Server Management Studio (SSMS)
-- Base de datos: dominospizza_RA5
-- ============================================================

ALTER TABLE tbl_ingrediente
    ADD unidad_medida VARCHAR(20) NULL;

-- Verificar que se agregó correctamente
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'tbl_ingrediente'
ORDER BY ORDINAL_POSITION;
