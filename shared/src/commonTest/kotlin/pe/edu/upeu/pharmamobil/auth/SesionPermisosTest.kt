package pe.edu.upeu.pharmamobil.auth

import pe.edu.upeu.pharmamobil.domain.model.Sesion
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SesionPermisosTest {

    @Test
    fun admin_cuentaConTodosLosPermisosDelSistema() {
        val sesionAdmin = Sesion(
            token = "jwt.token.admin",
            usuario = "admin",
            nombreCompleto = "Administrador del Sistema",
            permisos = setOf(
                "CATALOGO_LEER",
                "CATALOGO_ESCRIBIR",
                "CLIENTES_LEER",
                "CLIENTES_ESCRIBIR",
                "PEDIDOS_LEER",
                "PEDIDOS_ESCRIBIR"
            )
        )

        assertTrue(sesionAdmin.puede("CATALOGO_LEER"))
        assertTrue(sesionAdmin.puede("CATALOGO_ESCRIBIR"))
        assertTrue(sesionAdmin.puede("CLIENTES_LEER"))
        assertTrue(sesionAdmin.puede("CLIENTES_ESCRIBIR"))
        assertTrue(sesionAdmin.puede("PEDIDOS_LEER"))
        assertTrue(sesionAdmin.puede("PEDIDOS_ESCRIBIR"))
    }

    @Test
    fun vendedor_puedeVerCatalogoYPedidos_peroNoEditarCatalogo() {
        val sesionVendedor = Sesion(
            token = "jwt.token.vendedor",
            usuario = "vendedor",
            nombreCompleto = "Vendedor Farmacia",
            permisos = setOf(
                "CATALOGO_LEER",
                "CLIENTES_LEER",
                "CLIENTES_ESCRIBIR",
                "PEDIDOS_LEER",
                "PEDIDOS_ESCRIBIR"
            )
        )

        assertTrue(sesionVendedor.puede("CATALOGO_LEER"))
        assertFalse(sesionVendedor.puede("CATALOGO_ESCRIBIR"), "Vendedor no debe tener permiso de escritura en catálogo")
        assertTrue(sesionVendedor.puede("CLIENTES_LEER"))
        assertTrue(sesionVendedor.puede("PEDIDOS_LEER"))
    }

    @Test
    fun almacen_puedeGestionarCatalogo_peroNoVerClientes() {
        val sesionAlmacen = Sesion(
            token = "jwt.token.almacen",
            usuario = "almacen",
            nombreCompleto = "Encargado de Almacén",
            permisos = setOf(
                "CATALOGO_LEER",
                "CATALOGO_ESCRIBIR",
                "PEDIDOS_LEER"
            )
        )

        assertTrue(sesionAlmacen.puede("CATALOGO_LEER"))
        assertTrue(sesionAlmacen.puede("CATALOGO_ESCRIBIR"))
        assertFalse(sesionAlmacen.puede("CLIENTES_LEER"), "Almacén no debe tener acceso a clientes")
        assertFalse(sesionAlmacen.puede("CLIENTES_ESCRIBIR"))
    }

    @Test
    fun sesionSinPermisos_retornaFalsoParaCualquierAccion() {
        val sesionVacia = Sesion(
            token = "jwt.token.vacio",
            usuario = "usuario_base",
            nombreCompleto = "Usuario Base",
            permisos = emptySet()
        )

        assertFalse(sesionVacia.puede("CATALOGO_LEER"))
        assertFalse(sesionVacia.puede("CATALOGO_ESCRIBIR"))
        assertFalse(sesionVacia.puede("CLIENTES_LEER"))
        assertFalse(sesionVacia.puede("PEDIDOS_LEER"))
    }
}
