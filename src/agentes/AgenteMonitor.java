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
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import utilidad.MensajeConsola;

/**
 *
 * @author Hugo
 */
public class AgenteMonitor extends Agent        
{
    //Variables del agente
    private LinkedList<ACLMessage> mensajes;
    private TreeMap<AID, Integer> clasificacionMercados;
    private TreeMap<AID, Integer> clasificacionAgricultores;
    ConsolaJFrame gui;
    
    @Override
    protected void setup() {
       //Inicialización de las variables del agente
       mensajes = new LinkedList();
       clasificacionMercados = new TreeMap();
       clasificacionAgricultores = new TreeMap();
       //Configuración del GUI
       gui = new ConsolaJFrame(this);
       //Registro del agente en las Páginas Amarrillas
       DFAgentDescription dfd = new DFAgentDescription();
       dfd.setName(getAID());
       ServiceDescription sd = new ServiceDescription();
       sd.setType("Monitor");
       sd.setName("Monitor");
       dfd.addServices(sd);
       try
       {
           DFService.register(this, dfd);
       }
       catch (FIPAException e)
       {
           e.printStackTrace();
       }
       //Registro de la Ontología
       
       System.out.println("Se inicia la ejecución del agente: " + this.getName());
       //Añadir las tareas principales
       addBehaviour(new TareaClasificacion(this, 15000));
       addBehaviour(new TareaRecibirMensajes());
       addBehaviour(new TareaFinalizar(this, 30000));
    }

    @Override
    protected void takeDown() {
       //Desregristo del agente de las Páginas Amarillas
       try
       {
           DFService.deregister(this);
       }
       catch (FIPAException e)
       {
           e.printStackTrace();
       }
       //Liberación de recursos, incluido el GUI
       //Despedida
       System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }
    
    //Métodos de trabajo del agente
    
    
    //Clases internas que representan las tareas del agente
    
    /**
     * Tarea que se ejecutará cada vez que nos manden un mensaje tanto agricultores como mercados para comunicarnos su stock.
     */
    public class TareaRecibirMensajes extends CyclicBehaviour
    {
        public TareaRecibirMensajes()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage mensaje = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            if(mensaje != null)
            {
                String cnt;
                cnt = mensaje.getContent();
                int i = cnt.indexOf(":");
                String emisor = cnt.substring(0, i);
                int cantidad = Integer.parseInt(cnt.substring(i + 1, cnt.length()));
                
                if(emisor.equals("agricultor"))
                    clasificacionAgricultores.put(mensaje.getSender(), cantidad);
                else
                    clasificacionMercados.put(mensaje.getSender(), cantidad);
            }
            else
                block();
        }
    }
    
    /**
     * Tarea periódica que muestra la clasificación de mercados y agricultores.
     */
    public class TareaClasificacion extends TickerBehaviour
    {
        public TareaClasificacion(Agent a, long period)
        {
            super(a, period);
        }
        
        @Override
        protected void onTick()
        {
            String clasifAgric = ">>CLASIFICACION DE AGRICULTORES:\n";
            String clasifMerc = ">>CLASIFICACION DE MERCADOS:\n";
            
            ArrayList<Entry<AID, Integer>> tmp = new ArrayList();
            for(Entry<AID, Integer> entry : clasificacionAgricultores.entrySet())
            {
                tmp.add(entry);
            }
            tmp.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
            for(Entry<AID, Integer> entry : tmp)
            {
                clasifAgric = clasifAgric.concat("\tAgricultor " + entry.getKey().getName() + " con ganancias de " + entry.getValue() + "\n");
            }
            tmp.clear();
            
            for(Entry<AID, Integer> entry : clasificacionMercados.entrySet())
            {
                tmp.add(entry);
            }
            tmp.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
            for(Entry<AID, Integer> entry : tmp)
            {
                clasifMerc = clasifMerc.concat("\tMercado " + entry.getKey().getName() + " con stock de " + entry.getValue() + "\n");
            }
            
            gui.presentarSalida(new MensajeConsola(myAgent.getName(), "\n" + clasifAgric + clasifMerc + "\n"));
        }
    }
    
    /**
     * Tarea que finalizará a todos los agentes.
     */
    public class TareaFinalizar extends TickerBehaviour
    {
        public TareaFinalizar(Agent a, long periodo)
        {
            super(a, periodo);
        }
        
        @Override
        protected void onTick()
        {
            ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
            String[] servicios = {"Agricultor", "Mercado"};
            for(int i = 0; i < 2; ++i)
            {
                ServiceDescription servicio = new ServiceDescription(); //Buscamos el servicio especificado.
                servicio.setName(servicios[i]);

                // Plantilla de descripcin que busca el agente
                DFAgentDescription descripcion = new DFAgentDescription();

                // Servicio que busca el agente
                descripcion.addServices(servicio);
                try
                {
                    // Todas las descripciones que encajan con la plantilla proporcionada en el DF
                    DFAgentDescription[] resultados = DFService.search(myAgent, descripcion);
                    //Necesitaremos n espacios libres en el array, siendo n el nmero de agentes encontrados.
                    for(int j = 0; j < resultados.length; ++j)
                        msg.addReceiver(resultados[j].getName());

                }
                catch (FIPAException e)
                {
                    e.printStackTrace();
                }
            }
            
            myAgent.send(msg);
            myAgent.doDelete();
        }
    }
}
