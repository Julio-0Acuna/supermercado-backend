/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;
/**
 *
 * @author Julio Acuña
 */
import db.Conexion;
import dao.ProductoDAO;
import model.Producto;
import java.sql.*;
import java.util.UUID;

public class PruebasSupermercado {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println(" PRUEBAS EN CONJUNTO | BBDD SUPERMERCADO");
        System.out.println("===========================================");

        try {
            pruebaCRUDProducto();
            pruebaRecalculoBoletaYStock();
            pruebaErroresInesperados();
            System.out.println("\n✅✅✅ TODAS LAS PRUEBAS TERMINARON (revisa resultados arriba).");
        } catch (Exception e) {
            System.out.println("\n❌ Falló la ejecución general: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ PRUEBA 3: CRUD funciona (INSERT, SELECT, UPDATE, DELETE lógico)
    private static void pruebaCRUDProducto() {
        System.out.println("\n--- PRUEBA 3: CRUD PRODUCTO ---");
        ProductoDAO dao = new ProductoDAO();

        String codigo = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        Producto p = new Producto(codigo, "Producto Test", 999.0, 5);

        boolean ins = dao.insertar(p);
        System.out.println("INSERT: " + (ins ? "OK" : "FAIL") + " -> " + p);

        Producto encontrado = dao.buscarPorId(p.getIdProducto());
        System.out.println("SELECT por ID: " + (encontrado != null ? "OK" : "FAIL") + " -> " + encontrado);

        p.setNombre("Producto Test Editado");
        p.setPrecio(1111.0);
        p.setStock(7);
        boolean upd = dao.actualizar(p);
        System.out.println("UPDATE: " + (upd ? "OK" : "FAIL"));

        Producto actualizado = dao.buscarPorId(p.getIdProducto());
        System.out.println("SELECT después UPDATE: " + (actualizado != null ? "OK" : "FAIL") + " -> " + actualizado);

        boolean del = dao.eliminarLogico(p.getIdProducto());
        System.out.println("DELETE lógico (activo=0): " + (del ? "OK" : "FAIL"));

        Producto eliminado = dao.buscarPorId(p.getIdProducto());
        System.out.println("SELECT eliminado (debe existir pero activo=0): " +
                (eliminado != null ? "OK" : "FAIL") + " -> activo=" + (eliminado != null && eliminado.isActivo()));
    }

    // ✅ PRUEBA 1 y 2 juntas:
    // - Baja stock correctamente
    // - Recalcula subtotal/iva/total al insertar detalle
    private static void pruebaRecalculoBoletaYStock() throws SQLException {
        System.out.println("\n--- PRUEBA 1 + 2: STOCK + RECALCULO BOLETA ---");

        int idProducto = obtenerIdProductoParaPrueba();
        if (idProducto == -1) {
            System.out.println("❌ No hay productos en la tabla producto para probar.");
            return;
        }

        int stockAntes = leerStock(idProducto);
        System.out.println("Stock ANTES (producto " + idProducto + "): " + stockAntes);

        int idBoleta = crearBoletaPrueba();
        System.out.println("Boleta creada ID: " + idBoleta);

        // Insertar detalle (cantidad 1)
        insertarDetalle(idBoleta, idProducto, 1);

        int stockDespues = leerStock(idProducto);
        System.out.println("Stock DESPUÉS (debe bajar en 1): " + stockDespues);

        // Verificar totales recalculados
        BoletaTotales t = leerTotalesBoleta(idBoleta);
        System.out.println("Totales boleta -> subtotal: " + t.subtotal + " | iva: " + t.iva + " | total: " + t.total);

        if (stockAntes - stockDespues == 1) {
            System.out.println("✅ STOCK: OK (bajó 1)");
        } else {
            System.out.println("❌ STOCK: FAIL (no bajó como corresponde)");
        }

        if (t.subtotal > 0 && t.total > 0) {
            System.out.println("✅ RECALCULO: OK (totales > 0)");
        } else {
            System.out.println("❌ RECALCULO: FAIL (totales no se actualizaron)");
        }
    }

    // ✅ PRUEBA 4: No lanza errores inesperados (solo los esperados)
    private static void pruebaErroresInesperados() throws SQLException {
        System.out.println("\n--- PRUEBA 4: ERRORES ESPERADOS vs INESPERADOS ---");

        int idProducto = obtenerIdProductoParaPrueba();
        if (idProducto == -1) {
            System.out.println("❌ No hay productos para probar.");
            return;
        }

        int idBoleta = crearBoletaPrueba();

        int stock = leerStock(idProducto);
        int cantidadMayorAlStock = stock + 999;

        System.out.println("Probando venta con stock insuficiente...");
        try {
            insertarDetalle(idBoleta, idProducto, cantidadMayorAlStock);
            System.out.println("❌ FAIL: Debería haber fallado por stock insuficiente y no falló.");
        } catch (SQLException e) {
            // Esto es lo esperado por el SIGNAL del trigger
            System.out.println("✅ OK: Falló como se esperaba -> " + e.getMessage());
        }

        System.out.println("Probando descuento negativo (debe corregirse a 0 por trigger)...");
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE boleta SET descuento = -500 WHERE id_boleta = ?")) {
            ps.setInt(1, idBoleta);
            ps.executeUpdate();
        }

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT descuento FROM boleta WHERE id_boleta = ?")) {
            ps.setInt(1, idBoleta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double descuento = rs.getDouble("descuento");
                    System.out.println("Descuento guardado: " + descuento + " (debe ser 0)");
                    System.out.println(descuento == 0 ? "✅ OK" : "❌ FAIL");
                }
            }
        }
    }

    // -------------------- Helpers SQL --------------------

    private static int obtenerIdProductoParaPrueba() throws SQLException {
        // toma el primer producto activo con stock > 0
        String sql = "SELECT id_producto FROM producto WHERE activo = 1 AND stock > 0 ORDER BY id_producto LIMIT 1";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    private static int crearBoletaPrueba() throws SQLException {
        String folio = "BOL-JAVA-" + UUID.randomUUID().toString().substring(0, 8);
        String sql = "INSERT INTO boleta (folio, metodo_pago, monto_pagado, descuento) VALUES (?, 'EFECTIVO', 20000, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, folio);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo crear boleta de prueba.");
    }

    private static void insertarDetalle(int idBoleta, int idProducto, int cantidad) throws SQLException {
        // precio_unitario se toma desde la tabla producto para evitar inconsistencias
        double precio = leerPrecio(idProducto);

        String sql = "INSERT INTO boleta_detalle (id_boleta, id_producto, cantidad, precio_unitario, subtotal_linea) " +
                     "VALUES (?, ?, ?, ?, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBoleta);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            ps.setDouble(4, precio);
            ps.executeUpdate();
        }
    }

    private static int leerStock(int idProducto) throws SQLException {
        String sql = "SELECT stock FROM producto WHERE id_producto = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo leer stock.");
    }

    private static double leerPrecio(int idProducto) throws SQLException {
        String sql = "SELECT precio FROM producto WHERE id_producto = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        throw new SQLException("No se pudo leer precio.");
    }

    private static BoletaTotales leerTotalesBoleta(int idBoleta) throws SQLException {
        String sql = "SELECT subtotal, iva, total FROM boleta WHERE id_boleta = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBoleta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BoletaTotales(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
                }
            }
        }
        throw new SQLException("No se pudieron leer totales boleta.");
    }

    private record BoletaTotales(double subtotal, double iva, double total) {}
}