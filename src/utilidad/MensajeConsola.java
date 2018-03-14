/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

/**
 *
 * @author Hugo
 */
public class MensajeConsola
{
    private String nombre;
    private String mensaje;

    public MensajeConsola(String nombre, String mensaje)
    {
        this.nombre = nombre;
        this.mensaje = mensaje;
    }

    @Override
    public String toString()
    {
        return (nombre + "   " + mensaje);
    }

    public String getNombre()
    {
        return nombre;
    }

    public String getMensaje()
    {
        return mensaje;
    }
}