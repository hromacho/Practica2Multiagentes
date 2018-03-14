/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.LinkedList;
import gui.ConsolaJFrame;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import utilidad.MensajeConsola;

/**
 *
 * @author Hugo
 */
public class AgenteConsola extends Agent
{
    ConsolaJFrame gui;
    LinkedList<MensajeConsola> colaMensajes; //Cola de mensajes a presentar en pantalla.
    
    @Override
    protected void setup() 
    {
       //Inicializaci�n de las variables del agente
       gui = new ConsolaJFrame(this);
       colaMensajes = new LinkedList();
       
       //Registro del agente en las P�ginas Amarrillas
       DFAgentDescription dfd = new DFAgentDescription();
       dfd.setName(getAID());
       ServiceDescription sd = new ServiceDescription();
       sd.setType("GUI");
       sd.setName("Consola");
       dfd.addServices(sd);
       try
       {
           DFService.register(this, dfd);
       }
       catch (FIPAException e)
       {
           e.printStackTrace();
       }
       
       System.out.println("Se inicia la ejecuci�n del agente: " + this.getName());
       
       //A?adir las tareas principales
       addBehaviour(new TareaRecepcionMensajes());
    }

    @Override
    protected void takeDown() 
    {
       //Desregristo del agente de las P�ginas Amarillas
       try
       {
           DFService.deregister(this);
       }
       catch (FIPAException e)
       {
           e.printStackTrace();
       }
       //Liberaci�n de recursos, incluido el GUI
       gui.dispose();
       
       //Despedida
       System.out.println("Finaliza la ejecuci�n del agente: " + this.getName());
    }
    
    /**
     * Tarea que recoge los mensajes enviados al agente consola.
    */
    public class TareaRecepcionMensajes extends CyclicBehaviour
    {
        public TareaRecepcionMensajes()
        {
            super();
        }
        
        @Override
        public void action()
        {
            //Recibimos los mensajes de otros agentes, tanto como los de informaci�n como los de confirmaci�n.
            ACLMessage msg = receive();
            if(msg != null)
            {
                MensajeConsola mc = new MensajeConsola(msg.getSender().getName(), msg.getContent());
                colaMensajes.add(mc); //Lo a?adimos a la cola para su posterior procesamiento.
                addBehaviour(new TareaPresentarMensaje()); //Finalmente presentamos el mensaje en la interfaz.
            }
            else //En el caso de que no haya mensajes, bloqueamos la tarea para que no acapare toda la CPU.
                block();
        }
    }
    
    /**
     * Tarea encargada de presentar los mensajes en la interfaz gr�fica.
     */
    public class TareaPresentarMensaje extends OneShotBehaviour
    {
        public TareaPresentarMensaje()
        {
            super();
        }
        
        
        @Override
        public void action()
        {
            MensajeConsola mc = colaMensajes.pop(); //Cogemos el primer mensaje y lo borramos de la cola.
            gui.presentarSalida(mc); //Finalmente presentamos el mensaje en la interfaz.
        }
    }
}
