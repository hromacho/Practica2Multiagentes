/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tareas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Hugo
 * Tarea encargada de enviar peri�dicamente todos los mensajes disponibles de un agente a su consola correspondiente.
 */
public class TareaEnviarMensajeConsola extends TickerBehaviour
{
    private ArrayList<AID> consolas;
    private LinkedList<ACLMessage> mensajes;
    
    public TareaEnviarMensajeConsola(Agent a, long periodo, ArrayList<AID> consolas, LinkedList<ACLMessage> mensajes)
    {
        super(a, periodo);
        this.consolas = consolas;
        this.mensajes = mensajes;
    }
    
    @Override
    protected void onTick()
    {
        if(!consolas.isEmpty())
                if(!mensajes.isEmpty())
                {
                    for(ACLMessage msg : mensajes)
                        myAgent.send(msg);
                    mensajes.clear();
                }
    }
}
