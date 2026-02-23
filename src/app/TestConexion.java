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
import java.sql.Connection;

public class TestConexion {

public static void main(String[] args) {

        try (Connection con = Conexion.getConnection()) {
            System.out.println("Conexión exitosa a la base de datos 🚀");
        } catch (Exception e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }

    
}}
