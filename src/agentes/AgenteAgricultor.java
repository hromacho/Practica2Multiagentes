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
       //consolas = new ArrayList();
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
       gui.dispose();
       //Despedida
       System.out.println("Finaliza la ejecuci�n del agente: " + this.getName());
    }
    
    //M�todos de trabajo del agente
    
    
    //Clases internas que representan las tareas del agente
    
    //Esta va cada 5 segundos
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
            cosechas.add(new AtomicInteger(x)); //Solo uno sobrevivirá :D
            
            addBehaviour(new TareaVenderCosecha());
        }
    }
    
    //No sé cómo hacer esto.
    //Tal vez tenga que apuntarse en la páginas amarillas ofreciendo su servicio.
    //Creo que esto tiene que ser un ticker y pilla las cosechas de algún sitio. Wait a sec que piense.
    public class TareaVenderCosecha extends OneShotBehaviour //DUDEEE
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
                    int precio = cosechas.get(i).intValue(); //Ofertamos tooooodas las cosechas.
                    
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
                
                //Aunque los mercados hayan borrado ya la cosecha de su lista de ofertas, puede que aun así hayan llegado a pedir dicha cosecha.
                //En ese caso tenemos que encargarnos de ello. 
                int indice = -1; //Es probable que ya hayamos vendido nuestra cosecha a otro mercado.
                for(int i = 0; i < cosechas.size(); ++i)
                    if(cosechas.get(i).intValue() == precio)
                    {
                        indice = i;
                        break;
                    }
                        
                if(indice != -1)
                {
                    gui.presentarSalida(new MensajeConsola(myAgent.getName(), "Antes: " + cosechas.toString() + "\n"));
                    ganancia += cosechas.remove(indice).intValue();
                    gui.presentarSalida(new MensajeConsola(myAgent.getName(), "Despues: " + cosechas.toString() + "\n"));
                    ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM); //Enviamos un mensaje de confirmación de compra
                    msg.setSender(myAgent.getAID());
                    msg.addReceiver(mensaje.getSender()); 
                    msg.setContent(cnt);
                    send(msg);
                    
                    gui.presentarSalida(new MensajeConsola(myAgent.getName(), "->Agente agricultor vende cosecha por valor " + precio + " al mercado " + mensaje.getSender().getName() + "\n" + 
                            "\tTiene " + cosechas.size() + " cosechas  y ha ganado hasta ahora " + ganancia + "\n"));

                    addBehaviour(new TareaComunicarGanancias());
                }
                else
                {
                    
                }
            }
            else
                block();
        }
    }
    
    //Se ejecuta cada cierto tiempo y comprueba si hay nuevos mensajes de ganancias a mandar a la donde le corresponda.
    //Creo que podemos comunicar las ganancias cada vez que vendamos algo. Así se actualiza mejor.
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
}
