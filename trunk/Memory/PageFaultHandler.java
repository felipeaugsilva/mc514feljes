package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    static int frameNum = 0;    // frame que sera analisado no page replacement

    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
        int numFrames = MMU.getFrameTableSize();
        FrameTableEntry frame = null;
        boolean semMemSufic = true;
        boolean freeFrame = false;
        SystemEvent pfEvent = new SystemEvent("PageFault");
        thread.suspend(pfEvent);
        OpenFile swapFile;

        page.setValidatingThread(thread);

        // verifica se a pagina ja esta carregada
        if(page.isValid()) {
            pfEvent.notifyThreads();
            ThreadCB.dispatch();
            return FAILURE;
        }

        // verifica se ha algum frame que nao esteja 'travado' ou reservado
        for(int i = 0; i < numFrames; i++) {
            frame = MMU.getFrame(i);
            if(!frame.isReserved() || frame.getLockCount() <= 0)
                semMemSufic = false;
        }
        if(semMemSufic) {
            pfEvent.notifyThreads();
            ThreadCB.dispatch();
            return NotEnoughMemory;
        }

        // procura um frame livre
        for(int i = 0; i < numFrames && !freeFrame; i++) {
            frame = MMU.getFrame(i);
            if(frame.getPage() == null)
                freeFrame = true;
        }
        // se nao houver nenhum, chama pageReplacement
        if(!freeFrame)
            frame = pageReplacement(thread);

        // verifica se a thread foi morta enquanto esperava swap out de alguma pagina
        if(thread.getStatus() == ThreadKill) {
            pfEvent.notifyThreads();
            ThreadCB.dispatch();
            return FAILURE;
        }

        // realiza swap in da pagina solicitada
        swapFile = thread.getTask().getSwapFile();
        swapFile.read(page.getID(), page, thread);

        // verifica se a thread foi morta enquanto esperava swap in da pagina
        if(thread.getStatus() == ThreadKill) {
            pfEvent.notifyThreads();
            ThreadCB.dispatch();
            return FAILURE;
        }
        
        // atualiza page table
        page.setValidatingThread(null);
        page.setValid(true);
        page.setFrame(frame);

        // atualiza frame table
        frame.setUnreserved(thread.getTask());
        frame.setPage(page);
        frame.setDirty(false);
        frame.setReferenced(true);

        // notifica threads e chama dispatcher
        pfEvent.notifyThreads();
        ThreadCB.dispatch();

        return SUCCESS;
    }

    /*
     * Algoritmo para page replacement: Second Chance.
     * Supoe que ha pelo menos algum frame que nao esteja 'travado' ou reservado.
     * Utiliza variavel estatica frameNum para "lembrar" ultima pagina substituida.
     * Realiza swap out se a pagina vitima tiver sido modificada.
     * Retorna frame liberado.
     */
    public static FrameTableEntry pageReplacement(ThreadCB thread)
    {
        FrameTableEntry frame;
        PageTableEntry page;
        OpenFile swapFile;
        int numFrames = MMU.getFrameTableSize();

        // percorre frame table circularmente
        while(true) {

            frame = MMU.getFrame(frameNum);

            // frame 'travado' ou reservado
            if(frame.isReserved() || frame.getLockCount() > 0) {
                frameNum = (frameNum+1) % numFrames;
                continue;
            }
            // se o bit de referencia for 1, zera e vai pro proximo frame
            if(frame.isReferenced()) {
                frame.setReferenced(false);
                frameNum = (frameNum+1) % numFrames;
                continue;
            }
            // achou
            // se a pagina foi modificada, realiza swap out
            if(frame.isDirty()) {
                page = frame.getPage();
                swapFile = page.getTask().getSwapFile();
                swapFile.write(page.getID(), page, page.getTask().getCurrentThread());  //verificar terceiro argumento
                frame.setDirty(false);
            }
            page.setValid(false);
            frame.setUnreserved(thread.getTask());
            frame.setPage(null);
            return frame;
        }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
