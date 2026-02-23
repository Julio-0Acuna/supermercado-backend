/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

/**
 *
 * @author Julio Acuña
 */
import dao.ProductoDAO;
import model.Producto;
import java.util.List;
import java.util.Scanner;

public class MenuProducto {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ProductoDAO dao = new ProductoDAO();

        int op;
        do {
            System.out.println("\n=== SUPERMERCADO | CRUD PRODUCTOS ===");
            System.out.println("1) Listar productos");
            System.out.println("2) Agregar producto");
            System.out.println("3) Buscar producto por ID");
            System.out.println("4) Actualizar producto");
            System.out.println("5) Eliminar producto (lógico)");
            System.out.println("0) Salir");
            System.out.print("Opción: ");

            while (!sc.hasNextInt()) {
                System.out.print("Ingrese un número: ");
                sc.next();
            }
            op = sc.nextInt();
            sc.nextLine(); // limpiar salto

            switch (op) {
                case 1 -> listar(dao);
                case 2 -> agregar(sc, dao);
                case 3 -> buscar(sc, dao);
                case 4 -> actualizar(sc, dao);
                case 5 -> eliminar(sc, dao);
                case 0 -> System.out.println("Saliendo...");
                default -> System.out.println("Opción inválida.");
            }

        } while (op != 0);
    }

    private static void listar(ProductoDAO dao) {
        List<Producto> lista = dao.listarActivos();
        if (lista.isEmpty()) {
            System.out.println("No hay productos activos.");
            return;
        }
        System.out.println("\n--- Productos Activos ---");
        lista.forEach(System.out::println);
    }

    private static void agregar(Scanner sc, ProductoDAO dao) {
        System.out.print("Código de barra: ");
        String codigo = sc.nextLine().trim();

        System.out.print("Nombre: ");
        String nombre = sc.nextLine().trim();

        System.out.print("Precio: ");
        double precio = leerDouble(sc);

        System.out.print("Stock: ");
        int stock = leerInt(sc);

        Producto p = new Producto(codigo, nombre, precio, stock);
        boolean ok = dao.insertar(p);
        System.out.println(ok ? "✅ Producto agregado: " + p : "❌ No se pudo agregar (¿código repetido?).");
    }

    private static void buscar(Scanner sc, ProductoDAO dao) {
        System.out.print("ID a buscar: ");
        int id = leerInt(sc);

        Producto p = dao.buscarPorId(id);
        System.out.println(p != null ? "Encontrado: " + p : "No existe ese producto.");
    }

    private static void actualizar(Scanner sc, ProductoDAO dao) {
        System.out.print("ID a actualizar: ");
        int id = leerInt(sc);
        sc.nextLine();

        Producto actual = dao.buscarPorId(id);
        if (actual == null || !actual.isActivo()) {
            System.out.println("No existe o está inactivo.");
            return;
        }

        System.out.println("Actual: " + actual);

        System.out.print("Nuevo código de barra (" + actual.getCodigoBarra() + "): ");
        String codigo = sc.nextLine().trim();
        if (codigo.isEmpty()) codigo = actual.getCodigoBarra();

        System.out.print("Nuevo nombre (" + actual.getNombre() + "): ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) nombre = actual.getNombre();

        System.out.print("Nuevo precio (" + actual.getPrecio() + "): ");
        String precioTxt = sc.nextLine().trim();
        double precio = precioTxt.isEmpty() ? actual.getPrecio() : Double.parseDouble(precioTxt);

        System.out.print("Nuevo stock (" + actual.getStock() + "): ");
        String stockTxt = sc.nextLine().trim();
        int stock = stockTxt.isEmpty() ? actual.getStock() : Integer.parseInt(stockTxt);

        actual.setCodigoBarra(codigo);
        actual.setNombre(nombre);
        actual.setPrecio(precio);
        actual.setStock(stock);

        boolean ok = dao.actualizar(actual);
        System.out.println(ok ? "✅ Actualizado: " + actual : "❌ No se pudo actualizar.");
    }

    private static void eliminar(Scanner sc, ProductoDAO dao) {
        System.out.print("ID a eliminar (lógico): ");
        int id = leerInt(sc);

        boolean ok = dao.eliminarLogico(id);
        System.out.println(ok ? "✅ Eliminado (activo=0)." : "❌ No se pudo eliminar.");
    }

    private static int leerInt(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("Ingrese un entero válido: ");
            sc.next();
        }
        return sc.nextInt();
    }

    private static double leerDouble(Scanner sc) {
        while (!sc.hasNextDouble()) {
            System.out.print("Ingrese un número válido: ");
            sc.next();
        }
        return sc.nextDouble();
    }
}