/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import gui.ConsolaJFrame;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import tareas.TareaBuscarPaginasAmarillas;
import utilidad.MensajeConsola;

/**
 *
 * @author Hugo
 */
public class AgenteAgricultor extends Agent
{
    //Variables del agente
    private ArrayList<AID> mercados;
    private ArrayList<AID> monitores;
    private LinkedList<AtomicInteger> cosechas;
    private int ganancia;
    ConsolaJFrame gui;
    
    @Override
    protected void setup() {
       //Inicializaci�n de las variables del agente
       mercados = new ArrayList();
       monitores = new ArrayList();
       cosechas = new LinkedList();
       ganancia = 0;
       gui = new ConsolaJFrame(this);
       //Configuraci�n del GUI
       
       //Registro del agente en las P�ginas Amarrillas
       DFAgentDescription dfd = new DFAgentDescription();
       dfd.setName(getAID());
       ServiceDescription sd = new ServiceDescription();
       sd.setType("Venta");
       sd.setName("Agricultor");
       dfd.addServices(sd);
       try
       {
           DFService.register(this, dfd);
       }
       catch (FIPAException e)
       {
           e.printStackTrace();
       }
       //Registro de la Ontolog�a
       
       System.out.println("Se inicia la ejecuci�n del agente: " + this.getName());
       //A?adir las tareas principales
       addBehaviour(new TareaRecibirOferta());
       addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Mercado", mercados));
       addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Monitor", monitores));
       addBehaviour(new TareaRecolectarCosecha(this, 5000));
       addBehaviour(new TareaFinalizar());
    }

    @Override
    protected void takeDown() {
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
       //Despedida
       System.out.println("Finaliza la ejecuci�n del agente: " + this.getName());
    }
    
    //M�todos de trabajo del agente
    
    
    //Clases internas que representan las tareas del agente
    
    //Tarea con la que el agente agricultor recolecta una cosecha.
    public class TareaRecolectarCosecha extends TickerBehaviour
    {
        public TareaRecolectarCosecha(Agent a, long periodo)
        {
            super(a, periodo);
        }
        
        @Override
        protected void onTick()
        {
            Random rnd = new Random();
            int x = rnd.nextInt(6) + 5; //Entre 5 y 10
            cosechas.add(new AtomicInteger(x));
            
            addBehaviour(new TareaVenderCosecha());
        }
    }
    
    /**
     * Tarea que enviará un mensaje a todos los mercados con las cosechas disponibles.
     */
    public class TareaVenderCosecha extends OneShotBehaviour
    {
        public TareaVenderCosecha()
        {
            
        }
        
        @Override
        public void action()
        {
            if(!mercados.isEmpty()) //Si hay mercados disponibles...
            {
                for(int i = 0; i < cosechas.size(); ++i)
                {
                    int precio = cosechas.get(i).intValue(); //Ofertamos todas las cosechas.
                    
                    ACLMessage mens = new ACLMessage(ACLMessage.PROPOSE);

                    mens.setContent(Integer.toString(precio));
                    mens.setSender(myAgent.getAID());
                    
                    for (AID mercado : mercados)
                    {
                        mens.addReceiver(mercado);
                    }

                    send(mens);
                }
            }
        }
    }
    
    /**
     * Esta tarea se ejecutará cada vez que recibamos una oferta.
     */
    public class TareaRecibirOferta extends CyclicBehaviour
    {
        public TareaRecibirOferta()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage mensaje = receive(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
            if(mensaje != null)
            {
                String cnt;
                cnt = mensaje.getContent();
                int precio = Integer.parseInt(cnt);
                
                int indice = -1; //Es probable que ya hayamos vendido nuestra cosecha a otro mercado.
                for(int i = 0; i < cosechas.size(); ++i)
                    if(cosechas.get(i).intValue() == precio)
                    {
                        indice = i;
                        break;
                    }
                        
                if(indice != -1)
                {
                    ganancia += cosechas.remove(indice).intValue();
                    ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM); //Enviamos un mensaje de confirmación de compra
                    msg.setSender(myAgent.getAID());
                    msg.addReceiver(mensaje.getSender()); 
                    msg.setContent(cnt);
                    send(msg);
                    
                    gui.presentarSalida(new MensajeConsola(myAgent.getName(), "->Agente agricultor vende cosecha por valor " + precio + " al mercado " + mensaje.getSender().getName() + "\n" + 
                            "\tTiene " + cosechas.size() + " cosechas  y ha ganado hasta ahora " + ganancia + "\n"));

                    //Este mensaje va destinado a todos los mercados (menos del que va a comprar la cosecha) les indica que deben borrar la oferta de este
                    //agricultor porque ya ha sido vendida.
                    ACLMessage msg3 = new ACLMessage(ACLMessage.INFORM); 
                    msg3.setSender(myAgent.getAID());
                    for(AID mercado : mercados)
                        if(mercado != mensaje.getSender())
                            msg3.addReceiver(mercado);
                        
                    msg3.setContent(cnt);
                    send(msg3);
                    
                    addBehaviour(new TareaComunicarGanancias());
                }
            }
            else
                block();
        }
    }
    
    /**
     * Tarea que se ejecuta cuando un mercado ha aceptado una oferta pero no tiene fondos suficientes para comprarla.
     */
    public class TareaRechazarOferta extends CyclicBehaviour
    {
        public TareaRechazarOferta()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage mensaje = receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
            if(mensaje != null)
            {
                String cnt;
                cnt = mensaje.getContent();
                int precio = Integer.parseInt(cnt);
                
                //Devolvemos la cosecha con el resto
                cosechas.add(new AtomicInteger(precio));
                ganancia -= precio;
            }
            else
                block();
        }
    }
    
    /**
     * Tarea que comunica las ganancias a los monitores.
     */
    public class TareaComunicarGanancias extends OneShotBehaviour
    {
        public TareaComunicarGanancias()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM); //Enviamos nuestro mensaje a los monitores
            msg.setSender(myAgent.getAID());
            for(AID monitor : monitores)
                msg.addReceiver(monitor);
                
            msg.setContent("agricultor:" + Integer.toString(ganancia));
            send(msg);
        }
    }
    
    public class TareaFinalizar extends CyclicBehaviour
    {
        public TareaFinalizar()
        {

        }
        
        public void action()
        {
            ACLMessage mensaje = receive(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE));
            if(mensaje != null)
            {
                myAgent.doDelete();
            }
            else
                block();
        }
    }
}
