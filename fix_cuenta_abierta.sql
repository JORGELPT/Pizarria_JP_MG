USE dominospizza_RA5;
GO

-- 1) Asegurar que existe la columna estado en tbl_pedido
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'tbl_pedido' AND COLUMN_NAME = 'estado'
)
ALTER TABLE tbl_pedido ADD estado VARCHAR(30) DEFAULT 'pendiente';
GO

-- 2) Eliminar CHECK constraint de metodo_pago si existe
DECLARE @sql NVARCHAR(MAX);
SELECT @sql = 'ALTER TABLE tbl_pedido DROP CONSTRAINT ' + name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('tbl_pedido')
  AND COL_NAME(parent_object_id, parent_column_id) = 'metodo_pago';
IF @sql IS NOT NULL EXEC sp_executesql @sql;
GO

-- 3) Eliminar CHECK constraint de estado si existe
DECLARE @sql2 NVARCHAR(MAX);
SELECT @sql2 = 'ALTER TABLE tbl_pedido DROP CONSTRAINT ' + name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('tbl_pedido')
  AND COL_NAME(parent_object_id, parent_column_id) = 'estado';
IF @sql2 IS NOT NULL EXEC sp_executesql @sql2;
GO
