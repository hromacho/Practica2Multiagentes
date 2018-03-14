/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tareas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.ArrayList;

/**
 *
 * @author Hugo
 * Tarea encargada de buscar un servicio en las páginas amarillas dado por su nombre.
 */
public class TareaBuscarPaginasAmarillas extends TickerBehaviour
{
    private String service; //Nombre del servicio.
    private ArrayList<AID> agentes; //Agentes encontrados que ofrecen dicho servicio.
    
    /**
     * Constructor
     * @param a Agente que busca un servicio.
     * @param periodo Periodo de tiempo (en milisegundos) en el que se repetirá la tarea.
     * @param service Nombre del servicio a buscar.
     * @param agentes ArrayAgentes perteneciente al agente que busca y que almacena los agentes encontrados que ofrecen un servicio.
     */
    public TareaBuscarPaginasAmarillas(Agent a, long periodo, String service, ArrayList<AID> agentes)
    {
        super(a, periodo);
        this.service = service;
        this.agentes = agentes;
    }
    
    @Override
    protected void onTick()
    {
        ServiceDescription servicio = new ServiceDescription(); //Buscamos el servicio especificado.
        servicio.setName(service);

        // Plantilla de descripción que busca el agente
        DFAgentDescription descripcion = new DFAgentDescription();

        // Servicio que busca el agente
        descripcion.addServices(servicio);
        try
        {
            // Todas las descripciones que encajan con la plantilla proporcionada en el DF
            DFAgentDescription[] resultados = DFService.search(myAgent, descripcion);
            if(resultados.length > 0)
            {   //Necesitaremos n espacios libres en el array, siendo n el número de agentes encontrados.
                agentes.clear();
                for (int i = 0; i < resultados.length; ++i)
                    agentes.add(resultados[i].getName()); //Finalmente los almacenamos en el array.
                    
            }
            else
                agentes.clear(); 
                
        }
        catch (FIPAException e)
        {
            e.printStackTrace();
        }
    }
}
