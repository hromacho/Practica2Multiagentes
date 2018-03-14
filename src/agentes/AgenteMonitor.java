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
import tareas.TareaBuscarPaginasAmarillas;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import tareas.TareaEnviarMensajeConsola;
import utilidad.MensajeConsola;

/**
 *
 * @author Hugo
 */
public class AgenteMonitor extends Agent        
{
    //Variables del agente
    //private ArrayList<AID> agricultores;
    //private ArrayList<AID> mercados;
    private ArrayList<AID> consolas;
    private LinkedList<ACLMessage> mensajes;
    private TreeMap<AID, Integer> clasificacionMercados;
    private TreeMap<AID, Integer> clasificacionAgricultores;
    ConsolaJFrame gui;
    
    @Override
    protected void setup() {
       //Inicialización de las variables del agente
       //agricultores = new ArrayList();
       //mercados = new ArrayList();
       consolas = new ArrayList();
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
       //addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Agricultor", agricultores));
       //addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Mercado", mercados));
       addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Consola", consolas));
       addBehaviour(new TareaEnviarMensajeConsola(this, 2000, consolas, mensajes));
       addBehaviour(new TareaClasificacion(this, 15000));
       addBehaviour(new TareaRecibirMensajes());
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
       gui.dispose();
       //Despedida
       System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }
    
    //Métodos de trabajo del agente
    
    
    //Clases internas que representan las tareas del agente
    
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
                //System.out.println("Contenido del mensaje: " + cnt + "\n");
                int i = cnt.indexOf(":");
                String emisor = cnt.substring(0, i);
                int cantidad = Integer.parseInt(cnt.substring(i + 1, cnt.length()));
                //System.out.println("Emisor: " + emisor + "\tCantidad: " + Integer.toString(cantidad) + "\n");
                //TreeMap.SimpleEntry<AID, Integer> entry = new TreeMap.SimpleEntry(mensaje.getSender(), Integer.parseInt(cnt.substring(i + 1, cnt.length())));
                
                if(emisor.equals("agricultor"))
                    clasificacionAgricultores.put(mensaje.getSender(), cantidad);
                else
                    clasificacionMercados.put(mensaje.getSender(), cantidad);
                
                //System.out.println("Cantidad de agricultores: " + clasificacionAgricultores.size() + "\n");
                //System.out.println("Cantidad de mercados: " + clasificacionMercados.size() + "\n");
            }
            else
                block();
        }
    }
    
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
            tmp.sort((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()));
            for(Entry<AID, Integer> entry : tmp)
            {
                clasifAgric = clasifAgric.concat("\tAgricultor " + entry.getKey().getName() + " con ganancias de " + entry.getValue() + "\n");
            }
            tmp.clear();
            
            for(Entry<AID, Integer> entry : clasificacionMercados.entrySet())
            {
                tmp.add(entry);
            }
            tmp.sort((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()));
            for(Entry<AID, Integer> entry : tmp)
            {
                clasifMerc = clasifMerc.concat("\tMercado " + entry.getKey().getName() + " con stock de " + entry.getValue() + "\n");
            }
            
            
            if(!consolas.isEmpty())
            {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(consolas.get(0));

                msg.setContent(clasifAgric + clasifMerc + "\n");
                mensajes.add(msg);
            }
            gui.presentarSalida(new MensajeConsola(myAgent.getName(), clasifAgric + clasifMerc + "\n"));
        }
    }
}
