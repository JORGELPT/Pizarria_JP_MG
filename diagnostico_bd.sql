USE dominospizza_RA5;
GO

-- 1) Todas las tablas de la BD
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME;
GO

-- 2) Columnas de tbl_pedido (tipo, nullable, default)
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH,
       IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'tbl_pedido'
ORDER BY ORDINAL_POSITION;
GO

-- 3) CHECK constraints de tbl_pedido
SELECT name AS constraint_name,
       OBJECT_NAME(parent_object_id) AS table_name,
       COL_NAME(parent_object_id, parent_column_id) AS column_name,
       definition
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('tbl_pedido');
GO

-- 4) Datos de ejemplo para ver valores existentes
SELECT TOP 10 id_pedido, metodo_pago, estado, precio_total, fecha_pedido
FROM tbl_pedido
ORDER BY id_pedido DESC;
GO
