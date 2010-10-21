/*
* Grupo XY
* RA: 081704
* RA: 096993
*
* Status:
*
* 07/10/2010
* 1. Definição de algumas variáveis em ResourceCB e implementação da classe RRB.
*
*14/10/2010
*1. Feito o deadlock detection e o algoritmo do deadlock avoidance(falta ainda implementar a parte de alocacao de recursos para a thread)
*2. Feito o do_giveupresources 
*OBS: Nada foi testado ainda.
*
*19/10/2010
*1. Tentativa de verificar erros sem sucesso.
*
* */ 

package osp.Resources;

import java.util.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Memory.*;

/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/
public class ResourceCB extends IflResourceCB
{
    private static int available[] = new int[ResourceTable.getSize()];
    private static Hashtable<Integer, Integer> allocation[];
    private static Hashtable<Integer, Integer> request[];
    private static Hashtable<Integer, ThreadCB> threads = new Hashtable();
    private static Vector<RRB> RRBs;
    private static Hashtable<Integer, Integer> need[];
    private static Hashtable<Integer, Boolean> Finish[];

    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)
    {
        super(qty);
        available[this.getID()] = qty;

    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()
    {
        int numRecursos = ResourceTable.getSize();

        allocation = new Hashtable[numRecursos];
        request = new Hashtable[numRecursos];
        need = new Hashtable[numRecursos];
        RRBs = new Vector();
        Finish = new Hashtable[numRecursos];

        for(int i = 0; i < numRecursos; i++) {
            allocation[i] = new Hashtable();
            request[i] = new Hashtable();
            need[i] = new Hashtable();
            Finish[i] = new Hashtable();
        }
    }

    /**
       Tries to acquire the given quantity of this resource.
       Uses deadlock avoidance or detection depending on the
       strategy in use, as determined by ResourceCB.getDeadlockMethod().

       @param quantity
       @return The RRB corresponding to the request.
       If the request is invalid (quantity+allocated>total) then return null.

       @OSPProject Resources
    */
    public RRB do_acquire(int quantity) 
    {

        int i, numRecursos = ResourceTable.getSize();

        //Hashtable<Integer, Boolean> Finish[] = new Hashtable[numRecursos];
		
        boolean flag = true, flag1 = true;

	ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread(), auxThread;
		
	int work, alocado, necessario = ( this.getMaxClaim(thread) - this.getAllocated(thread) );

        int id = this.getID();
   
        int n = allocation[id].size(), threadID;

        Enumeration keys_alloc = allocation[id].keys();

        Enumeration keys_finish, elems_finish;

        Enumeration keys_need;

        RRB rrb = new RRB(thread, this, quantity);

        
          //inicializar o vetor need

        while(keys_alloc.hasMoreElements()){
          auxThread = threads.get((Integer)keys_alloc.nextElement());
          need[id].put(auxThread.getID(), (this.getMaxClaim(auxThread) - this.getAllocated(auxThread)));
        }

        while(keys_alloc.hasMoreElements()) 
          Finish[id].put((Integer)(keys_alloc.nextElement()), false);

        keys_finish = Finish[id].keys();
        elems_finish = Finish[id].elements();

        keys_alloc = allocation[id].keys();
        keys_need = need[id].keys();
        
		
        
	if( quantity <= ( this.getMaxClaim(thread) - this.getAllocated(thread) ) )
	{
	  if(quantity <= this.getAvailable())
	  {
            if( ResourceCB.getDeadlockMethod() == Avoidance )
	    {
	      work = this.getAvailable() - quantity;
	      alocado = this.getAllocated(thread);
              //allocation[id].put(thread.getID(), (this.getAllocated(thread) + quantity));
              if(keys_need.hasMoreElements()){
                  if(need[id].contains(thread.getID()))need[id].put(thread.getID(), (need[id].get(thread.getID()) - quantity));
                  else need[id].put(thread.getID(), quantity);
              }
              else need[id].put(thread.getID(), quantity);
              //if(need[id].get(thread.getID()) != null) necessario = need[id].get(thread.getID()) - quantity;
              //else necessario = quantity;

	      //while(keys_alloc.hasMoreElements())
	      while(keys_finish.hasMoreElements())
              {
                threadID = (Integer)keys_alloc.nextElement();
		if(!((Boolean)(elems_finish.nextElement())))
		{
                    if (work >= need[id].get(thread.getID()) )
                    {
                       if(thread.getID() == threadID) work = work + alocado;
                       else work = work + this.getAllocated(threads.get(threadID));
                       Finish[id].put(threadID, true);
                       keys_alloc = allocation[id].keys();
                       elems_finish = Finish[id].elements();
                    }

		}
				
	      }

              elems_finish = Finish[id].elements();
	      while(elems_finish.hasMoreElements() && flag)
              {
                if(!((Boolean)(elems_finish.nextElement()))) flag = false;
              }
	    } //banqueiro
	  }
          else
	  {
	    //processo deve esperar
	    rrb.setStatus(Suspended);
	    thread.suspend(rrb);
            if(request[id].keys().hasMoreElements()) {
                if(request[id].contains(thread.getID()))
                    request[id].put(thread.getID(), request[id].get(thread.getID()) + quantity);
                else
                    request[id].put(thread.getID(), quantity);
            }
            else
                request[id].put(thread.getID(), quantity);
	    threads.put(thread.getID(), thread);
            RRBs.add(rrb);
	    return rrb;
	  }
        }
        else return null;//processo excedeu o máximo pedido

        if(flag)
        {
          //sistema em estado seguro	
          // atualiza allocation
          // atualiza available
          // rrb.do_grant()
            
          if(allocation[id].get(thread.getID()) != null)
              allocation[id].put(thread.getID(), (allocation[id].get(thread.getID()) + quantity));
          else
              allocation[id].put(thread.getID(), quantity);
          available[id] = this.getAvailable() - quantity;
          rrb.grant();
          threads.put(thread.getID(), thread);
        }
        else
        {
          // sistema em estado inseguro	
          // nao aloca, retem o estado
          // processo deve esperar??
 	  rrb.setStatus(Suspended);
	  thread.suspend(rrb);
          RRBs.add(rrb);
	  request[id].put(thread.getID(), quantity);
	  threads.put(thread.getID(), thread);
        }
        
        return rrb;
    }

    /**
       Performs deadlock detection.
       @return A vector of ThreadCB objects found to be in a deadlock.

       @OSPProject Resources
    */
    public static Vector do_deadlockDetection()
    {
        Vector<ThreadCB> threadsEmDeadlock = new Vector();
        int threadID;
        int numRecursos = ResourceTable.getSize();
        int work[] = new int[numRecursos];
        Hashtable<Integer, Boolean> finish = new Hashtable();
        RRB rrb;
        Enumeration en;

        System.arraycopy(available, 0, work, 0, numRecursos);   // work = available

        en = threads.keys();
        while(en.hasMoreElements()){
            threadID = (Integer)en.nextElement();
            for(int i = 0; i < numRecursos; i++) {
                if(allocation[i].containsKey(threadID)) {
                    if(!finish.containsKey(threadID))
                        finish.put(threadID, true);
                    if(allocation[i].get(threadID) != 0)
                        finish.put(threadID, false);
                }
            }
        }

        /*for(int i = 0; i < numRecursos; i++) {
            en = allocation[i].keys();
            while(en.hasMoreElements()) {
                threadID = (Integer)en.nextElement();
                if(!finish.containsKey(threadID))
                    finish.put(threadID, true);
                if(allocation[i].get(threadID) != 0)
                    finish.put(threadID, false);
            }
        }*/

        boolean fim = false;

        test:
        while(!fim) {
            en = finish.keys();
            while(en.hasMoreElements()) {
                threadID =(Integer)en.nextElement();
                if(!finish.get(threadID) && ResourceCB.requestMenorWork(work, threadID)) {
                    for(int i = 0; i < numRecursos; i++) {
                        if(allocation[i].get(threadID) != null)
                            work[i] += allocation[i].get(threadID);
                    }
                    finish.put(threadID, true);
                    continue test;
                }
            }
            fim = true;
        }

        ThreadCB thread = null;

        // verifica se as threads conseguirao ser finalizadas
        // se nao, insere no vetor de threads em deadlock
        en = finish.keys();
        while(en.hasMoreElements()) {
            threadID = (Integer)en.nextElement();
            if(!finish.get(threadID))
                threadsEmDeadlock.add(threads.get(threadID));
        }

        // nao ha threads em Deadlock
        if(threadsEmDeadlock.isEmpty())
            return null;

        // mata uma thread e chama do_deadlockDetection recursivamente
        threadsEmDeadlock.firstElement().kill();
        ResourceCB.do_deadlockDetection();

        return threadsEmDeadlock;
    }

    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
        Enumeration en;
        ResourceCB recurso;
        RRB rrb;

        // libera todos os recursos alocados para a thread
        for(int i = 0; i < ResourceTable.getSize(); i++) {
            recurso = ResourceTable.getResourceCB(i);
            recurso.setAvailable(recurso.getAvailable() + recurso.getAllocated(thread));
            recurso.setAllocated(thread, 0);
            available[i] = recurso.getAvailable();
            allocation[i].remove(thread.getID());
        }

        // remove a thread do lista de RRBs, caso ela esteja na lista
        en = RRBs.elements();
        while(en.hasMoreElements()) {
            rrb = (RRB)en.nextElement();
            if(rrb.getThread().getID() == thread.getID())
                RRBs.remove(rrb);
        }

        // remove thread da lista de threads
        threads.remove(thread.getID());

        // verifica se ha algum RRB que pode ter seus recursos alocados
        en = RRBs.elements();
        while(en.hasMoreElements()) {
            rrb = (RRB)en.nextElement();
            recurso = rrb.getResource();
            if(rrb.getQuantity() <= recurso.getAvailable()) {
                rrb.grant();
                available[recurso.getID()] = recurso.getAvailable();
                allocation[recurso.getID()].put(rrb.getThread().getID(), recurso.getAllocated(rrb.getThread()));
                RRBs.remove(rrb);
            }
        }
    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)
    {
        ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread(), auxThread;
        
        int id = this.getID(), quant;
        
        RRB rrb = null;

        ResourceCB recurso;

        Enumeration e = RRBs.elements();
        
        Boolean flag = true;


        
        this.setAvailable( (this.getAvailable() + quantity) );
        this.setAllocated(thread, (this.getAllocated(thread) - quantity) );
        
        available[id] = this.getAvailable();
        allocation[id].put(thread.getID(), this.getAllocated(thread));

        //será que não tem que tirar o rrb aqui ?
        
        while(e.hasMoreElements() && flag){
            rrb = (RRB)e.nextElement();
            auxThread = rrb.getThread();
            if(thread.getID() == auxThread.getID()) flag = false;
        }

        e = RRBs.elements();


        if(this.getAllocated(thread) == 0)  RRBs.remove(rrb);
        
        while(e.hasMoreElements()) {
            rrb = (RRB)e.nextElement();
            recurso = rrb.getResource();
            if(rrb.getQuantity() <= recurso.getAvailable()) {
                rrb.grant();
                available[recurso.getID()] = recurso.getAvailable();
                allocation[recurso.getID()].put(rrb.getThread().getID(), recurso.getAllocated(rrb.getThread()));
                RRBs.remove(rrb);
            }
        }

        /*while(e.hasMoreElements()) {
            rrb = (RRB)e.nextElement();
            quant = rrb.getQuantity();
            if(quant <= available[id]) {
                rrb.grant();
                available[id] -= quant;
                allocation[id].put(thread.getID(), quant);
                RRBs.remove(rrb);
            }
        }*/
                
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

    /*  Método auxiliar (do_deadlockDetection): retorna 'true' se
        'request' < 'work'. Em vez de usar o próprio 'request', utiliza
        os rrbs suspensos.
    */
    public static boolean requestMenorWork(int[] work, int threadID)
    {
        Enumeration en = RRBs.elements();
        RRB rrb;
        
        while(en.hasMoreElements()){
            rrb = (RRB)en.nextElement();
            if(rrb.getThread().getID() == threadID && rrb.getQuantity() > work[rrb.getResource().getID()])
                return false;
        }
        return true;
    }

}

/*
      Feel free to add local classes to improve the readability of your code
*/
