/*
* Grupo 05
* RA: 081704
* RA: 096993
*
*04/11/2010
* 1. Feito o MMU
* 2. Feito o PageTable
* 3. Começou o PageFaultHandler
* 4. Nada foi testado ainda.
 *
 *11/11/2010
 * 1. Todas as classes implementadas só falta testar.
*/


package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import java.lang.Math;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        int numFrames = MMU.getFrameTableSize();
        FrameTableEntry frame;
        
        for (int i = 0; i < numFrames; i++){
            frame = new FrameTableEntry(i);
            MMU.setFrame(i, frame);
        }
        MyOut.print("osp.Memory.MMU", "init MMU");

    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {

       int i, end = 0;
       int aux = MMU.getVirtualAddressBits() - MMU.getPageAddressBits();
       int tamPage = (int)Math.pow(2, aux);
       PageTable pt = MMU.getPTBR();       
       SystemEvent pfEvent = new SystemEvent("PageFault");

       MyOut.print("osp.Memory.MMU", "endereço de memória " + memoryAddress);
       MyOut.print("osp.Memory.MMU", "tamanho pag " + tamPage);

       end = memoryAddress/tamPage;

       MyOut.print("osp.Memory.MMU", "num pag " + end);

       if( pt.pages[end].isValid() ) {                                           //pagina valida
           pt.pages[end].getFrame().setDirty(true);
           pt.pages[end].getFrame().setReferenced(true);
           //return pt.pages[end];
       }
       else {                                                                    //pagina invalida
           if(pt.pages[end].getValidatingThread() != null){                      //thread tentando referenciar esta pagian e causou pagefault
               thread.suspend(pt.pages[end]);
               if(pt.pages[end].isValid()){
                   if(thread.getStatus() != ThreadKill) {
                       pt.pages[end].getFrame().setDirty(true);
                       pt.pages[end].getFrame().setReferenced(true);
                       //return pt.pages[end];
                   }
               }
               else {
                   //return pt.pages[end];
               }

           }
           else {                                                                //nenhuma thread referenciou esta pagina ainda entao deve ser causado um pagefault
               InterruptVector.setPage(pt.pages[end]);
               InterruptVector.setReferenceType(referenceType);
               InterruptVector.setThread(thread);
               CPU.interrupt(PageFault);
               if(thread.getStatus() != ThreadKill) {
                   pt.pages[end].getFrame().setDirty(true);
                   pt.pages[end].getFrame().setReferenced(true);
                   //return pt.pages[end];
               }
               else {
                   //return pt.pages[end];
               }
           }
       }
       MyOut.print("osp.Memory.MMU", "shift depois");
       return pt.pages[end];
   }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
