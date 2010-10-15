package osp.Resources;

import java.util.*;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Threads.*;

/**
   The studends module for dealing with resource management. The methods 
   that have to be implemented are do_grant().

   @OSPProject Resources
*/

public class RRB extends IflRRB
{
    /** 
        constructor of class RRB 
        Creates a new RRB object. This constructor must have
        super() as its first statement.

        @OSPProject Resources
    */   
    public RRB(ThreadCB thread, ResourceCB resource,int quantity)
    {
        super(thread, resource, quantity);

    }

    /**
       This method is called when we decide to grant an RRB.
       The method must update the various resource quantities
       and notify the thread waiting on the granted RRB.

        @OSPProject Resources
    */
    public void do_grant()
    {
         RRB rrb;                        // deve apontar para o que? -- ver isso
         ThreadCB thread;
         ResourceCB resource;
         int qty;

         thread = this.getThread();      // thread que pediu recursos
         resource = this.getResource();  // qual recurso foi pedido
         qty = this.getQuantity();       // quantidade do recurso pedido


         resource.setAvailable(resource.getAvailable() - qty);              
         resource.setAllocated(thread, resource.getAllocated(thread) + qty);
         this.setStatus(Granted);
         this.notifyThreads();           //sinaliza para que a thread possa resumir

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
