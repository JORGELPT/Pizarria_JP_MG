-- ============================================================
--  DIAGNÓSTICO + FIX COMPLETO DE PRECIOS
--  Base de datos: dominospizza_RA5
--  Ejecutar completo en SSMS con F5
-- ============================================================

USE dominospizza_RA5;
GO

-- ============================================================
-- PASO 1: Ver qué hay actualmente
-- ============================================================
PRINT '=== PRODUCTOS SIN PRECIO (antes del fix) ===';
SELECT
    p.id_producto,
    p.nombre,
    p.tipo,
    ISNULL(MIN(pp.costo), 0) AS precio_actual,
    COUNT(pp.id_presentacion) AS filas_precio
FROM tbl_producto p
LEFT JOIN tbl_presentacion_producto pp ON pp.id_producto = p.id_producto
WHERE p.disponibilidad = 1
GROUP BY p.id_producto, p.nombre, p.tipo
ORDER BY filas_precio ASC, p.tipo, p.nombre;
GO

-- ============================================================
-- PASO 2: Asegurarse de que tbl_presentacion tiene al menos 1 fila
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM tbl_presentacion)
BEGIN
    INSERT INTO tbl_presentacion (presentacion, pedazo)
    VALUES ('Único', 1);
    PRINT 'Se creó presentación base.';
END
GO

-- ============================================================
-- PASO 3: FIX - Insertar precio para cada producto que NO
--          tiene NINGUNA fila en tbl_presentacion_producto.
--          El precio se asigna según el tipo (columna p.tipo).
-- ============================================================
DECLARE @pres INT = (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion);
PRINT 'Usando id_presentacion = ' + CAST(@pres AS VARCHAR);

INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT
    p.id_producto,
    @pres,
    CASE
        WHEN LOWER(p.tipo) LIKE '%pizza%'     THEN 360.00
        WHEN LOWER(p.tipo) LIKE '%pasta%'     THEN 280.00
        WHEN LOWER(p.tipo) LIKE '%alita%'     THEN 320.00
        WHEN LOWER(p.tipo) LIKE '%bebida%'    THEN  80.00
        WHEN LOWER(p.tipo) LIKE '%postre%'    THEN 150.00
        WHEN LOWER(p.tipo) LIKE '%ensalada%'  THEN 200.00
        WHEN LOWER(p.tipo) LIKE '%sandwich%'  THEN 220.00
        WHEN LOWER(p.tipo) LIKE '%acompa%'    THEN 180.00  -- acompañamiento
        ELSE                                       150.00  -- genérico
    END
FROM tbl_producto p
WHERE p.disponibilidad = 1
  AND NOT EXISTS (
      SELECT 1
      FROM tbl_presentacion_producto x
      WHERE x.id_producto = p.id_producto
  );

PRINT CAST(@@ROWCOUNT AS VARCHAR) + ' producto(s) con precio nuevo insertado(s).';
GO

-- ============================================================
-- PASO 4: Actualizar los que sí tienen fila pero costo = 0 o NULL
-- ============================================================
DECLARE @pres INT = (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion);

UPDATE pp
SET pp.costo =
    CASE
        WHEN LOWER(p.tipo) LIKE '%pizza%'     THEN 360.00
        WHEN LOWER(p.tipo) LIKE '%pasta%'     THEN 280.00
        WHEN LOWER(p.tipo) LIKE '%alita%'     THEN 320.00
        WHEN LOWER(p.tipo) LIKE '%bebida%'    THEN  80.00
        WHEN LOWER(p.tipo) LIKE '%postre%'    THEN 150.00
        WHEN LOWER(p.tipo) LIKE '%ensalada%'  THEN 200.00
        WHEN LOWER(p.tipo) LIKE '%sandwich%'  THEN 220.00
        WHEN LOWER(p.tipo) LIKE '%acompa%'    THEN 180.00
        ELSE                                       150.00
    END
FROM tbl_presentacion_producto pp
JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE (pp.costo IS NULL OR pp.costo = 0)
  AND pp.id_presentacion = @pres;

PRINT CAST(@@ROWCOUNT AS VARCHAR) + ' precio(s) en $0 corregido(s).';
GO

-- ============================================================
-- PASO 5: Precios específicos por nombre (refinamiento)
-- ============================================================
-- Pizzas
UPDATE pp SET pp.costo = 350.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%margarita%' OR LOWER(p.nombre) LIKE '%margherita%';

UPDATE pp SET pp.costo = 370.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%pepperoni%';

UPDATE pp SET pp.costo = 380.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%suprema%' OR LOWER(p.nombre) LIKE '%especial%';

UPDATE pp SET pp.costo = 360.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%hawayana%' OR LOWER(p.nombre) LIKE '%hawaiana%' OR LOWER(p.nombre) LIKE '%hawai%';

UPDATE pp SET pp.costo = 375.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%provolone%' OR LOWER(p.nombre) LIKE '%provolon%' OR LOWER(p.nombre) LIKE '%provolo%';

UPDATE pp SET pp.costo = 375.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%bbq%';

-- Alitas
UPDATE pp SET pp.costo = 320.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%alita%';

-- Bebidas
UPDATE pp SET pp.costo = 60.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%refresco%' OR LOWER(p.nombre) LIKE '%soda%' OR LOWER(p.nombre) LIKE '%cola%';

UPDATE pp SET pp.costo = 120.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%jugo%' OR LOWER(p.nombre) LIKE '%limonada%';

UPDATE pp SET pp.costo = 45.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%agua%';

-- Acompañamientos
UPDATE pp SET pp.costo = 250.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%mozzarella%' OR LOWER(p.nombre) LIKE '%palito%';

UPDATE pp SET pp.costo = 90.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) = 'pan';

UPDATE pp SET pp.costo = 180.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%pan de ajo%';

UPDATE pp SET pp.costo = 320.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%miel mostaza%';

UPDATE pp SET pp.costo = 320.00
FROM tbl_presentacion_producto pp JOIN tbl_producto p ON p.id_producto = pp.id_producto
WHERE LOWER(p.nombre) LIKE '%picante%';

PRINT 'Refinamiento por nombres completado.';
GO

-- ============================================================
-- PASO 6: VERIFICACIÓN FINAL
-- ============================================================
PRINT '=== RESULTADO FINAL ===';
SELECT
    p.nombre                                AS Producto,
    p.tipo                                  AS Tipo,
    FORMAT(MIN(pp.costo), 'N2')             AS [Precio RD$],
    COUNT(pp.id_presentacion)               AS [Filas precio],
    CASE
        WHEN MIN(pp.costo) IS NULL OR MIN(pp.costo) = 0
        THEN '❌ SIN PRECIO'
        ELSE '✅ OK'
    END                                     AS Estado
FROM tbl_producto p
LEFT JOIN tbl_presentacion_producto pp ON pp.id_producto = p.id_producto
WHERE p.disponibilidad = 1
GROUP BY p.id_producto, p.nombre, p.tipo
ORDER BY Estado DESC, p.tipo, p.nombre;
GO
