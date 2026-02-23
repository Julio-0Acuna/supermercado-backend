/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Julio Acuña
 */
import java.time.LocalDateTime;

public class Producto {
    private int idProducto;
    private String codigoBarra;
    private String nombre;
    private double precio;
    private int stock;
    private boolean activo;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public Producto() {}

    public Producto(String codigoBarra, String nombre, double precio, int stock) {
        this.codigoBarra = codigoBarra;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.activo = true;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    @Override
    public String toString() {
        return String.format("ID:%d | %s | %s | $%.2f | stock:%d | activo:%s",
                idProducto, codigoBarra, nombre, precio, stock, activo ? "SI" : "NO");
    }
}
