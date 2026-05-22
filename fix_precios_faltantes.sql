-- ====================================================================
--  FIX: Productos que muestran $0.00 en Hacer un Pedido
--  Referencia de precios existentes en la BD:
--    Pizzas    ~$350-380  |  Alitas BBQ  $320
--    Bebidas   $60-120    |  (se mantienen coherentes)
--  Ejecutar en SQL Server Management Studio sobre dominospizza_RA5
-- ====================================================================

USE dominospizza_RA5;
GO

-- Obtener el id_presentacion base (el primer tamaño registrado en tbl_presentacion)
DECLARE @pres INT = (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion);

PRINT 'Usando id_presentacion = ' + CAST(@pres AS VARCHAR);

-- ====================================================================
-- Tabla de precios para los productos sin precio
-- (nombres exactos como aparecen en tbl_producto)
-- ====================================================================
;WITH precios_nuevos AS (
    SELECT nombre, precio FROM (VALUES
        ('Palitos de Mozzarella', 250.00),  -- acompañamiento
        ('pan',                    90.00),  -- pan genérico
        ('Pan de Ajo',            180.00),  -- acompañamiento
        ('Pizza grande hawayana', 360.00),  -- igual rango que demás pizzas
        ('pizza provolonne',      375.00),  -- pizza premium
        ('Alitas Miel Mostaza',   320.00),  -- igual que Alitas BBQ
        ('Alitas Picantes',       320.00)   -- igual que Alitas BBQ
    ) AS t(nombre, precio)
)
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, @pres, pn.precio
FROM  precios_nuevos pn
JOIN  tbl_producto   p  ON LOWER(p.nombre) = LOWER(pn.nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_presentacion_producto x
    WHERE x.id_producto = p.id_producto
      AND x.id_presentacion = @pres
);

PRINT CAST(@@ROWCOUNT AS VARCHAR) + ' registro(s) insertado(s).';

-- ====================================================================
-- VERIFICACIÓN: muestra el catálogo completo con precios resultantes
-- ====================================================================
SELECT
    p.nombre                                AS Producto,
    p.tipo                                  AS Tipo,
    FORMAT(MIN(pp.costo), 'N2')             AS [Precio RD$],
    CASE WHEN MIN(pp.costo) IS NULL OR MIN(pp.costo) = 0
         THEN '⚠ SIN PRECIO'
         ELSE '✅ OK'
    END                                     AS Estado
FROM  tbl_producto p
LEFT  JOIN tbl_presentacion_producto pp ON pp.id_producto = p.id_producto
WHERE p.disponibilidad = 1
GROUP BY p.id_producto, p.nombre, p.tipo
ORDER BY Estado DESC, p.tipo, p.nombre;
GO
