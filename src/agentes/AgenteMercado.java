/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

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
import tareas.TareaBuscarPaginasAmarillas;
import tareas.TareaEnviarMensajeConsola;

/**
 *
 * @author Hugo
 */
public class AgenteMercado extends Agent 
{
    private class Oferta
    {
        public AID vendedor;
        public int precio;
    };
    int fondos;
    int cosechas;
    private ArrayList<AID> consolas;
    private ArrayList<AID> monitores;
    private LinkedList<Oferta> ofertas;
    private LinkedList<ACLMessage> mensajes;

    @Override
    protected void setup() 
    {
        //Inicialización de las variables del agente
        fondos = 0;
        consolas = new ArrayList();
        monitores = new ArrayList();
        ofertas = new LinkedList();
        //Configuración del GUI

        //Registro del agente en las Páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Compra");
        sd.setName("Mercado");
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
        addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Consola", consolas));
        addBehaviour(new TareaBuscarPaginasAmarillas(this, 5000, "Monitor", monitores));
        addBehaviour(new TareaEnviarMensajeConsola(this, 2000, consolas, mensajes));
        addBehaviour(new TareaRecibirInversiones(this, 3000));
        addBehaviour(new TareaComprarCosecha(this, 6000));
        addBehaviour(new TareaConfirmarCompra());
        addBehaviour(new TareaBorrarOferta());
        addBehaviour(new TareaRecibirOfertas());
        
    }

    @Override
    protected void takeDown() 
    {
        //Desregristo del agente de las Páginas Amarillas

        //Liberación de recursos, incluido el GUI
        //Despedida
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    //Métodos de trabajo del agente
    //Clases internas que representan las tareas del agente
    public class TareaRecibirInversiones extends TickerBehaviour 
    {

        public TareaRecibirInversiones(Agent a, long periodo) 
        {
            super(a, periodo);
        }

        @Override
        protected void onTick() 
        {
            Random rnd = new Random();
            fondos += rnd.nextInt(7) + 2; //Entre 2 y 8
        }
    }
    
    public class TareaRecibirOfertas extends CyclicBehaviour
    {
        public TareaRecibirOfertas()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage mensaje = receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            if(mensaje != null)
            {
                String cnt;
                cnt = mensaje.getContent();
                int precio = Integer.parseInt(cnt);
                
                if(precio <= fondos)
                {
                    Oferta ofer = new Oferta();
                    ofer.precio = precio;
                    ofer.vendedor = mensaje.getSender();
                    ofertas.add(ofer);
                }
            }
            else
                block();
        }
    }
    
    public class TareaComprarCosecha extends TickerBehaviour
    {
        public TareaComprarCosecha(Agent a, long periodo)
        {
            super(a, periodo);
        }
        
        @Override
        protected void onTick()
        {
            if(!ofertas.isEmpty())
            {
                //Calcula la cosecha con el mejor precio.
                Oferta min = ofertas.get(0);
                int indice = 0;
                for(int i = 0; i < ofertas.size(); ++i)
                {
                    if(ofertas.get(i).precio < min.precio && min.precio <= fondos)
                    {
                        min = ofertas.get(i);
                        indice = i;
                    }
                }
                ofertas.remove(indice);
                
                ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(min.vendedor);
                msg.setContent(Integer.toString(min.precio));
                send(msg);
            }
        }
    }
    
    public class TareaConfirmarCompra extends CyclicBehaviour
    {
        public TareaConfirmarCompra()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage confirmacion = receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
            //ACLMessage rechaza = receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
            if(confirmacion != null)
            {
                String cnt;
                cnt = confirmacion.getContent();
                int precio = Integer.parseInt(cnt);
                
                if(precio <= fondos)
                {
                    fondos -= precio;
                    ++cosechas;
                    
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setSender(myAgent.getAID());
                    msg.addReceiver(consolas.get(0));
                    msg.setContent("->Agente mercado " + myAgent.getName() + " compra cosecha por valor " + precio + " al agricultor " + confirmacion.getSender().getName() + "\n");
                    mensajes.add(msg);
                    
                    addBehaviour(new TareaComunicarStock());
                }
                else //Si no tenemos dinero... tendremos que rechazar la oferta
                {
                    ACLMessage cancelar = new ACLMessage(ACLMessage.CANCEL);
                    cancelar.setSender(myAgent.getAID());
                    cancelar.addReceiver(confirmacion.getSender());
                    cancelar.setContent(cnt);
                    send(cancelar);
                }
            }
            //else if(rechaza != null)
            //{
                //Enviar mensaje a consola?
            //}
            else
                block();
        }
    }
    
    public class TareaBorrarOferta extends CyclicBehaviour
    {
        public TareaBorrarOferta()
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
                int precio = Integer.parseInt(cnt);
                
                int index = -1;
                for(int i = 0; i < ofertas.size(); ++i)
                {
                    if(ofertas.get(i).vendedor.equals(mensaje.getSender()) && ofertas.get(i).precio == precio)
                    {
                        index = i;
                        break;
                    }
                }
                
                if(index != -1)
                    ofertas.remove(index);
                //Y con esto, las siguientes veces que consideremos la mejor oferta de las almacenadas, no consideraremos ofertas potencialmente inexistentes.
            }
            else
                block();
        }
    }
    
    
    public class TareaComunicarStock extends OneShotBehaviour
    {
        public TareaComunicarStock()
        {
            
        }
        
        @Override
        public void action()
        {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM); //Enviamos nuestro mensaje a los monitores
            msg.setSender(myAgent.getAID());
            for(AID monitor : monitores)
                msg.addReceiver(monitor);
            msg.setContent(Integer.toString(cosechas));
            send(msg);
        }
    }
}
