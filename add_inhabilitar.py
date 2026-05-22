import os

BASE = r"C:\Users\jorge\IdeaProjects\demo1\src\main\java\com\example\demo1\Controllers"

CONFIGS = {
    "CONTROLLER_Cargo":         ("tbl_cargo",         "activo",         "nombre",  "TXTnombre.getText().trim()",  "Ingrese el nombre del cargo."),
    "CONTROLLER_Cliiente":      ("tbl_cliente",        "activo",         "cedula",  "TXTcedula.getText().trim()",  "Ingrese la cedula del cliente."),
    "CONTROLLER_ComprasPago":   ("tbl_compra",         "activo",         "id",      "idCompraSeleccionada",        "Seleccione una compra de la tabla primero."),
    "CONTROLLER_Comprobante":   ("tbl_itbs",           "activo",         "codigo",  "Txtcodigo.getText().trim()",  "Ingrese el codigo del comprobante."),
    "CONTROLLER_Departamento":  ("tbl_departamento",   "activo",         "nombre",  "TXTnombre.getText().trim()",  "Ingrese el nombre del departamento."),
    "CONTROLLER_Empleado":      ("tbl_empleado",       "activo",         "id",      "idEmpleadoSeleccionado",      "Seleccione un empleado de la tabla primero."),
    "CONTROLLER_Devolucion":    ("tbl_devolucion",     "activo",         "id",      "idDevolucionSeleccionado",    "Seleccione una devolucion de la tabla primero."),
    "CONTROLLER_Envio":         ("tbl_envio",          "activo",         "id",      "idEnvioSeleccionado",         "Seleccione un envio de la tabla primero."),
    "CONTROLLER_Fallosmaquina": ("tbl_fallo",          "activo",         "id",      "idFallo",                    "Use el boton buscar para cargar el fallo primero."),
    "CONTROLLER_Ingrediente":   ("tbl_ingrediente",    "activo",         "id",      "idIngredienteActual",         "Seleccione un ingrediente de la tabla o use buscar."),
    "CONTROLLER_Mantenimiento": ("tbl_mantenimiento",  "activo",         "id",      "idMantenimiento",             "Use el boton buscar para buscar el mantenimiento primero."),
    "CONTROLLER_Maquina":       ("tbl_maquina",        "activo",         "maquina", "TXTnombre.getText().trim()",  "Ingrese el nombre de la maquina."),
    "CONTROLLER_Producto":      ("tbl_producto",       "disponibilidad", "id",      "idProductoActual",            "Seleccione un producto de la tabla o use buscar."),
    "CONTROLLER_Proveedor":     ("tbl_proveedor",      "activo",         "prov",    "TXTnombre.getText().trim()",  "Ingrese el nombre del proveedor."),
    "CONTROLLER_Sucursal":      ("tbl_sucursal",       "activo",         "suc",     "TXTnombre.getText().trim()",  "Ingrese el nombre de la sucursal."),
    "CONTROLLER_Tecnico":       ("tbl_tecnico",        "activo",         "cedula",  "TXTcedula.getText().trim()",  "Ingrese la cedula del tecnico."),
}

def make_method(tabla, col, tipo, campo, msg):
    confirm_msg = "Inhabilitar este registro? No se eliminara, solo quedara inactivo."
    id_col = "id_" + tabla.replace("tbl_", "")

    if tipo == "id":
        body = (
            f'        if ({campo} == -1) {{\n'
            f'            JOptionPane.showMessageDialog(null, "{msg}");\n'
            f'            return;\n'
            f'        }}\n'
            f'        int confirmar = JOptionPane.showConfirmDialog(null, "{confirm_msg}", "Confirmar", JOptionPane.YES_NO_OPTION);\n'
            f'        if (confirmar != JOptionPane.YES_OPTION) return;\n'
            f'        try (java.sql.Connection con = Conexion.establecerConexion();\n'
            f'             java.sql.PreparedStatement ps = con.prepareStatement("UPDATE {tabla} SET {col} = 0 WHERE {id_col} = ?")) {{\n'
            f'            ps.setInt(1, {campo});\n'
            f'            ps.executeUpdate();\n'
            f'            JOptionPane.showMessageDialog(null, "Registro inhabilitado correctamente.");\n'
            f'            {campo} = -1;\n'
            f'        }} catch (Exception e) {{\n'
            f'            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());\n'
            f'        }}'
        )
    elif tipo == "cedula":
        body = (
            f'        String cedula = {campo};\n'
            f'        if (cedula.isEmpty()) {{\n'
            f'            JOptionPane.showMessageDialog(null, "{msg}");\n'
            f'            return;\n'
            f'        }}\n'
            f'        int confirmar = JOptionPane.showConfirmDialog(null, "{confirm_msg}", "Confirmar", JOptionPane.YES_NO_OPTION);\n'
            f'        if (confirmar != JOptionPane.YES_OPTION) return;\n'
            f'        String sql = "UPDATE {tabla} SET {col} = 0 WHERE id_persona = (SELECT id_persona FROM tbl_persona WHERE cedula = ?)";\n'
            f'        try (java.sql.Connection con = Conexion.establecerConexion();\n'
            f'             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {{\n'
            f'            ps.setString(1, cedula);\n'
            f'            ps.executeUpdate();\n'
            f'            JOptionPane.showMessageDialog(null, "Registro inhabilitado correctamente.");\n'
            f'        }} catch (Exception e) {{\n'
            f'            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());\n'
            f'        }}'
        )
    elif tipo == "codigo":
        body = (
            f'        String idStr = {campo};\n'
            f'        if (idStr.isEmpty()) {{\n'
            f'            JOptionPane.showMessageDialog(null, "{msg}");\n'
            f'            return;\n'
            f'        }}\n'
            f'        int confirmar = JOptionPane.showConfirmDialog(null, "{confirm_msg}", "Confirmar", JOptionPane.YES_NO_OPTION);\n'
            f'        if (confirmar != JOptionPane.YES_OPTION) return;\n'
            f'        try (java.sql.Connection con = Conexion.establecerConexion();\n'
            f'             java.sql.PreparedStatement ps = con.prepareStatement("UPDATE {tabla} SET {col} = 0 WHERE {id_col} = ?")) {{\n'
            f'            ps.setInt(1, Integer.parseInt(idStr));\n'
            f'            ps.executeUpdate();\n'
            f'            JOptionPane.showMessageDialog(null, "Registro inhabilitado correctamente.");\n'
            f'        }} catch (Exception e) {{\n'
            f'            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());\n'
            f'        }}'
        )
    else:  # nombre variants
        col_where = {"maquina": "nombre_maquina", "prov": "nombre_proveedor", "suc": "nombre_sucursal"}.get(tipo, "nombre")
        body = (
            f'        String nombre = {campo};\n'
            f'        if (nombre.isEmpty()) {{\n'
            f'            JOptionPane.showMessageDialog(null, "{msg}");\n'
            f'            return;\n'
            f'        }}\n'
            f'        int confirmar = JOptionPane.showConfirmDialog(null, "Inhabilitar \'" + nombre + "\'? No se eliminara, solo quedara inactivo.", "Confirmar", JOptionPane.YES_NO_OPTION);\n'
            f'        if (confirmar != JOptionPane.YES_OPTION) return;\n'
            f'        try (java.sql.Connection con = Conexion.establecerConexion();\n'
            f'             java.sql.PreparedStatement ps = con.prepareStatement("UPDATE {tabla} SET {col} = 0 WHERE {col_where} = ?")) {{\n'
            f'            ps.setString(1, nombre);\n'
            f'            ps.executeUpdate();\n'
            f'            JOptionPane.showMessageDialog(null, "Registro inhabilitado correctamente.");\n'
            f'        }} catch (Exception e) {{\n'
            f'            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());\n'
            f'        }}'
        )

    return f'\n    @FXML\n    public void FnInhabilitar() {{\n{body}\n    }}\n'

errors = []
for ctrl, (tabla, col, tipo, campo, msg) in CONFIGS.items():
    path = os.path.join(BASE, ctrl + ".java")
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if "FnInhabilitar" in content:
        print(f"SKIP (ya existe): {ctrl}")
        continue

    marker = 'JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage())'
    idx = content.rfind(marker)
    if idx == -1:
        errors.append(f"NO ANCHOR: {ctrl}")
        continue

    close = content.find("\n    }\n", idx)
    if close == -1:
        errors.append(f"NO CLOSE: {ctrl}")
        continue

    insert_pos = close + len("\n    }\n")
    method = make_method(tabla, col, tipo, campo, msg)
    new_content = content[:insert_pos] + method + content[insert_pos:]

    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print(f"OK: {ctrl}")

if errors:
    print("ERRORES:", errors)
