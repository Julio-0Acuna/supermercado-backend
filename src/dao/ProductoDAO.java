/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

/**
 *
 * @author Julio Acuña
 */
import db.Conexion;
import model.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // 1) INSERT
    public boolean insertar(Producto p) {
        String sql = "INSERT INTO producto (codigo_barra, nombre, precio, stock, activo) VALUES (?, ?, ?, ?, 1)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getCodigoBarra());
            ps.setString(2, p.getNombre());
            ps.setDouble(3, p.getPrecio());
            ps.setInt(4, p.getStock());

            int filas = ps.executeUpdate();
            if (filas == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setIdProducto(rs.getInt(1));
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Error insertar: " + e.getMessage());
            return false;
        }
    }

    // 2) SELECT (listar activos)
    public List<Producto> listarActivos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id_producto, codigo_barra, nombre, precio, stock, activo " +
                     "FROM producto WHERE activo = 1 ORDER BY nombre";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearBasico(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error listar: " + e.getMessage());
        }
        return lista;
    }

    // 3) SELECT por id
    public Producto buscarPorId(int id) {
        String sql = "SELECT id_producto, codigo_barra, nombre, precio, stock, activo " +
                     "FROM producto WHERE id_producto = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearBasico(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error buscarPorId: " + e.getMessage());
        }
        return null;
    }

    // 4) UPDATE
    public boolean actualizar(Producto p) {
        String sql = "UPDATE producto SET codigo_barra=?, nombre=?, precio=?, stock=? WHERE id_producto=? AND activo=1";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getCodigoBarra());
            ps.setString(2, p.getNombre());
            ps.setDouble(3, p.getPrecio());
            ps.setInt(4, p.getStock());
            ps.setInt(5, p.getIdProducto());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error actualizar: " + e.getMessage());
            return false;
        }
    }

    // 5) DELETE lógico (activo = 0)
    public boolean eliminarLogico(int id) {
        String sql = "UPDATE producto SET activo = 0 WHERE id_producto = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error eliminar: " + e.getMessage());
            return false;
        }
    }

    private Producto mapearBasico(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setCodigoBarra(rs.getString("codigo_barra"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecio(rs.getDouble("precio"));
        p.setStock(rs.getInt("stock"));
        p.setActivo(rs.getInt("activo") == 1);
        return p;
    }
    
    public Producto buscarPorCodigoBarra(String codigo) {
    String sql = "SELECT id_producto, codigo_barra, nombre, precio, stock, activo " +
                 "FROM producto WHERE codigo_barra = ?";
    try (Connection con = Conexion.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, codigo);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapearBasico(rs);
        }
    } catch (SQLException e) {
        System.out.println("Error buscarPorCodigoBarra: " + e.getMessage());
    }
    return null;
}

public int stockActual(int idProducto) {
    String sql = "SELECT stock FROM producto WHERE id_producto = ?";
    try (Connection con = Conexion.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idProducto);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("stock");
        }
    } catch (SQLException e) {
        System.out.println("Error stockActual: " + e.getMessage());
    }
    return -1;
}
}