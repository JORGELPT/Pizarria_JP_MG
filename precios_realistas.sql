-- ====================================================================
--  SCRIPT: Precios Realistas — dominospizza_RA5
--  Precios en Pesos Dominicanos (RD$) — Referencia Domino's DO 2026
--  Ejecutar en SQL Server Management Studio sobre dominospizza_RA5
-- ====================================================================

USE dominospizza_RA5;
GO

-- ====================================================================
-- PASO 1: Asegurar que tbl_presentacion tiene las 4 presentaciones base
-- ====================================================================

IF NOT EXISTS (SELECT 1 FROM tbl_presentacion WHERE presentacion LIKE '%equeñ%' OR presentacion LIKE '%equeno%')
    INSERT INTO tbl_presentacion (presentacion, pedazo) VALUES ('Pequeño', 6);

IF NOT EXISTS (SELECT 1 FROM tbl_presentacion WHERE presentacion LIKE '%edian%')
    INSERT INTO tbl_presentacion (presentacion, pedazo) VALUES ('Mediana', 8);

IF NOT EXISTS (SELECT 1 FROM tbl_presentacion WHERE presentacion LIKE '%rande%')
    INSERT INTO tbl_presentacion (presentacion, pedazo) VALUES ('Grande', 10);

IF NOT EXISTS (SELECT 1 FROM tbl_presentacion WHERE presentacion LIKE '%amiliar%')
    INSERT INTO tbl_presentacion (presentacion, pedazo) VALUES ('Familiar', 12);
GO

-- ====================================================================
-- PASO 2: IDs de presentaciones (variables reutilizables)
-- ====================================================================

DECLARE @pq INT, @md INT, @gr INT, @fm INT;

SELECT @pq = id_presentacion FROM tbl_presentacion
    WHERE presentacion LIKE '%equeñ%' OR presentacion LIKE '%equeno%';
SELECT @md = id_presentacion FROM tbl_presentacion
    WHERE presentacion LIKE '%edian%';
SELECT @gr = id_presentacion FROM tbl_presentacion
    WHERE presentacion LIKE '%rande%';
SELECT @fm = id_presentacion FROM tbl_presentacion
    WHERE presentacion LIKE '%amiliar%';

-- ====================================================================
-- HELPER: Aplica o actualiza precio de una presentación para un producto
-- Busca el producto por nombre (LIKE) para no depender de IDs hardcodeados
-- ====================================================================

-- -----------------------------------------------------------------------
--  PIZZAS  (4 tamaños: Pequeño / Mediana / Grande / Familiar)
--  Precios RD$ basados en carta Domino's República Dominicana 2026
-- -----------------------------------------------------------------------

DECLARE @id INT;

-- ── Pizza Pepperoni ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%pepperoni%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,495.00),(@id,@md,749.00),(@id,@gr,969.00),(@id,@fm,1249.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza Pepperoni';
END ELSE PRINT 'AVISO — Pizza Pepperoni no encontrada en tbl_producto';

-- ── Pizza Margarita ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%margarita%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,449.00),(@id,@md,685.00),(@id,@gr,885.00),(@id,@fm,1149.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza Margarita';
END ELSE PRINT 'AVISO — Pizza Margarita no encontrada en tbl_producto';

-- ── Pizza 4 Quesos ───────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%4 queso%' OR LOWER(nombre) LIKE '%cuatro queso%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,529.00),(@id,@md,795.00),(@id,@gr,1025.00),(@id,@fm,1329.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza 4 Quesos';
END ELSE PRINT 'AVISO — Pizza 4 Quesos no encontrada en tbl_producto';

-- ── Pizza BBQ Pollo ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%bbq%' AND LOWER(nombre) LIKE '%pollo%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,519.00),(@id,@md,779.00),(@id,@gr,999.00),(@id,@fm,1299.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza BBQ Pollo';
END ELSE PRINT 'AVISO — Pizza BBQ Pollo no encontrada en tbl_producto';

-- ── Pizza Hawaiana ───────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%hawai%' OR LOWER(nombre) LIKE '%hawaya%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,495.00),(@id,@md,749.00),(@id,@gr,969.00),(@id,@fm,1249.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza Hawaiana';
END ELSE PRINT 'AVISO — Pizza Hawaiana no encontrada en tbl_producto';

-- ── Pizza Provolone ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%provol%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,549.00),(@id,@md,819.00),(@id,@gr,1059.00),(@id,@fm,1379.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza Provolone';
END ELSE PRINT 'AVISO — Pizza Provolone no encontrada en tbl_producto';

-- ── Pizza Salami ─────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%salami%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,489.00),(@id,@md,739.00),(@id,@gr,955.00),(@id,@fm,1235.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pizza Salami';
END ELSE PRINT 'AVISO — Pizza Salami no encontrada en tbl_producto';

-- -----------------------------------------------------------------------
--  ALITAS  (2 tamaños: Pequeño=6 pzs / Grande=12 pzs)
-- -----------------------------------------------------------------------

-- ── Alitas BBQ ───────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%alita%' AND LOWER(nombre) LIKE '%bbq%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,425.00),(@id,@gr,779.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Alitas BBQ';
END ELSE PRINT 'AVISO — Alitas BBQ no encontradas en tbl_producto';

-- ── Alitas Miel Mostaza ───────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%miel%' OR LOWER(nombre) LIKE '%mostaza%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,425.00),(@id,@gr,779.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Alitas Miel Mostaza';
END ELSE PRINT 'AVISO — Alitas Miel Mostaza no encontradas en tbl_producto';

-- ── Alitas Picantes ───────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%picante%' AND LOWER(nombre) LIKE '%alita%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,425.00),(@id,@gr,779.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Alitas Picantes';
END ELSE PRINT 'AVISO — Alitas Picantes no encontradas en tbl_producto';

-- -----------------------------------------------------------------------
--  ACOMPAÑAMIENTOS  (precio único → presentación Pequeño)
-- -----------------------------------------------------------------------

-- ── Palitos de Mozzarella ─────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%palito%' OR LOWER(nombre) LIKE '%mozzarella%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,325.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Palitos de Mozzarella';
END ELSE PRINT 'AVISO — Palitos de Mozzarella no encontrados en tbl_producto';

-- ── Pan de Ajo ────────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%pan%ajo%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,215.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pan de Ajo';
END ELSE PRINT 'AVISO — Pan de Ajo no encontrado en tbl_producto';

-- ── Pan (genérico) ────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) = 'pan'
       OR (LOWER(nombre) LIKE '%pan%' AND LOWER(nombre) NOT LIKE '%ajo%'
           AND LOWER(nombre) NOT LIKE '%pizza%');
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,95.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Pan';
END ELSE PRINT 'AVISO — Pan no encontrado en tbl_producto';

-- -----------------------------------------------------------------------
--  BEBIDAS  (precio único → presentación Pequeño)
-- -----------------------------------------------------------------------

-- ── Coca-Cola 355 ml ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%coca%' AND (LOWER(nombre) LIKE '%355%' OR LOWER(nombre) LIKE '%lata%');
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,99.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Coca-Cola 355ml';
END ELSE PRINT 'AVISO — Coca-Cola 355ml no encontrada en tbl_producto';

-- ── Coca-Cola 600 ml ──────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto
    WHERE LOWER(nombre) LIKE '%coca%' AND (LOWER(nombre) LIKE '%600%' OR LOWER(nombre) LIKE '%botell%');
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,155.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Coca-Cola 600ml';
END ELSE PRINT 'AVISO — Coca-Cola 600ml no encontrada en tbl_producto';

-- ── Sprite ────────────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%sprite%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,99.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Sprite';
END ELSE PRINT 'AVISO — Sprite no encontrada en tbl_producto';

-- ── Agua Mineral ──────────────────────────────────────────────────────────
SELECT @id = id_producto FROM tbl_producto WHERE LOWER(nombre) LIKE '%agua%';
IF @id IS NOT NULL
BEGIN
    MERGE tbl_presentacion_producto AS t
    USING (VALUES (@id,@pq,79.00))
          AS s(ip,ipr,c) ON t.id_producto=s.ip AND t.id_presentacion=s.ipr
    WHEN MATCHED     THEN UPDATE SET t.costo=s.c
    WHEN NOT MATCHED THEN INSERT(id_producto,id_presentacion,costo) VALUES(s.ip,s.ipr,s.c);
    PRINT 'OK — Agua Mineral';
END ELSE PRINT 'AVISO — Agua Mineral no encontrada en tbl_producto';

-- -----------------------------------------------------------------------
--  PASO 3: FALLBACK POR TIPO
--  Cubre TODOS los productos que no tuvieron coincidencia por nombre.
--  Asigna precio según su columna "tipo" en tbl_producto.
--  Se insertan las 4 presentaciones para pizza/pasta,
--  y solo Pequeño para bebida/postre/otros.
-- -----------------------------------------------------------------------

-- Pizzas sin ninguna presentación registrada → 4 tamaños
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, pr.id_presentacion,
    CASE pr.presentacion
        WHEN 'Pequeño'  THEN 495.00
        WHEN 'Mediana'  THEN 749.00
        WHEN 'Grande'   THEN 969.00
        WHEN 'Familiar' THEN 1249.00
        ELSE 495.00
    END
FROM  tbl_producto p
CROSS JOIN tbl_presentacion pr
WHERE LOWER(p.tipo) = 'pizza'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x
      WHERE x.id_producto = p.id_producto AND x.id_presentacion = pr.id_presentacion
  );
PRINT 'Fallback pizza — presentaciones insertadas';

-- Pastas sin precio → 2 tamaños (Pequeño / Grande)
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, pr.id_presentacion,
    CASE pr.presentacion
        WHEN 'Pequeño' THEN 395.00
        WHEN 'Grande'  THEN 695.00
        ELSE 395.00
    END
FROM  tbl_producto p
CROSS JOIN tbl_presentacion pr
WHERE LOWER(p.tipo) IN ('pasta','pastas')
  AND pr.presentacion IN ('Pequeño','Grande')
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x
      WHERE x.id_producto = p.id_producto AND x.id_presentacion = pr.id_presentacion
  );
PRINT 'Fallback pasta — presentaciones insertadas';

-- Alitas sin precio → 2 tamaños (Pequeño=6pzs / Grande=12pzs)
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, pr.id_presentacion,
    CASE pr.presentacion
        WHEN 'Pequeño' THEN 425.00
        WHEN 'Grande'  THEN 779.00
        ELSE 425.00
    END
FROM  tbl_producto p
CROSS JOIN tbl_presentacion pr
WHERE LOWER(p.tipo) IN ('alita','alitas','pollo')
  AND pr.presentacion IN ('Pequeño','Grande')
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x
      WHERE x.id_producto = p.id_producto AND x.id_presentacion = pr.id_presentacion
  );
PRINT 'Fallback alitas — presentaciones insertadas';

-- Bebidas sin precio → solo Pequeño
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, @pq, 99.00
FROM  tbl_producto p
WHERE LOWER(p.tipo) IN ('bebida','bebidas','refresco','refrescos')
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x WHERE x.id_producto = p.id_producto
  );
PRINT 'Fallback bebidas — presentaciones insertadas';

-- Postres sin precio → solo Pequeño
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, @pq, 225.00
FROM  tbl_producto p
WHERE LOWER(p.tipo) IN ('postre','postres','dulce','dulces','dessert')
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x WHERE x.id_producto = p.id_producto
  );
PRINT 'Fallback postres — presentaciones insertadas';

-- CUALQUIER otro producto disponible que aún no tenga precio → Pequeño a RD$150
INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo)
SELECT p.id_producto, @pq, 150.00
FROM  tbl_producto p
WHERE p.disponibilidad = 1
  AND NOT EXISTS (
      SELECT 1 FROM tbl_presentacion_producto x WHERE x.id_producto = p.id_producto
  );
PRINT 'Fallback general — productos restantes asignados a RD$150';

-- -----------------------------------------------------------------------
--  PASO 4: Corrección final de seguridad
--  Si aún queda algún costo en 0 o NULL → mínimo RD$95
-- -----------------------------------------------------------------------
UPDATE tbl_presentacion_producto
SET    costo = 95.00
WHERE  costo IS NULL OR costo = 0;

PRINT '----------------------------------------------------';
PRINT 'Corrección final: registros con costo 0/NULL → 95.00';

-- ====================================================================
-- VERIFICACIÓN FINAL — ver resultado en pantalla
-- ====================================================================
SELECT
    p.id_producto,
    p.nombre                                          AS Producto,
    p.tipo                                            AS Tipo,
    pr.presentacion                                   AS Presentacion,
    FORMAT(pp.costo, 'N2')                            AS [Precio RD$]
FROM   tbl_producto p
JOIN   tbl_presentacion_producto pp ON pp.id_producto    = p.id_producto
JOIN   tbl_presentacion          pr ON pr.id_presentacion = pp.id_presentacion
WHERE  p.disponibilidad = 1
ORDER  BY p.tipo, p.nombre, pr.id_presentacion;
GO

PRINT '====================================================';
PRINT 'Script de precios completado exitosamente.';
PRINT '====================================================';
GO
