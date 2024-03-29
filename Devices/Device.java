/*
* Grupo 05
* RA: 081704
* RA: 096993
*
* 02/12/2010
 *1. Feito todas as classes.
 *2. Projeto rodando.
 *
 */

package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/
import java.lang.Math;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{

    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */

    public Device(int id, int numberOfBlocks)
    {
         super(id, numberOfBlocks);
         this.iorbQueue = new GenericList();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
       
    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
        PageTableEntry page;
        OpenFile openfile;
        ThreadCB thread;

        int blocks = MMU.getVirtualAddressBits() - MMU.getPageAddressBits();
        int tamblocks = (int)Math.pow(2, blocks);

        int SectorBytes = ((Disk)this).getBytesPerSector();
        int TrackSectors = ((Disk)this).getSectorsPerTrack();
        int CylinderTracks = ((Disk)this).getPlatters();

        int blocos = SectorBytes * TrackSectors * CylinderTracks /tamblocks ;
        int Cylinder = iorb.getBlockNumber() / blocos;
        
        page = iorb.getPage();
        openfile = iorb.getOpenFile();
        thread = iorb.getThread();

        if(page.lock(iorb) == FAILURE) return FAILURE;
        
        openfile.incrementIORBCount();
        iorb.setCylinder(Cylinder);

        if(thread.getStatus() != ThreadKill) {
            if(!this.isBusy())
                this.startIO(iorb);
            else
                ((GenericList)this.iorbQueue).insert(iorb);
            return SUCCESS;
        }
        else    // thread morreu
            return FAILURE;
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
        if(this.iorbQueue.isEmpty())
            return null;

        return (IORB)((GenericList)this.iorbQueue).removeTail(); // FIFO

    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
        IORB iorb;
        OpenFile openFile;
        Enumeration en = ((GenericList)this.iorbQueue).forwardIterator();
        
        while(en.hasMoreElements()) {

            iorb = (IORB)en.nextElement();
            openFile = iorb.getOpenFile();

            if(iorb.getThread() == thread) {

                iorb.getPage().unlock();

                openFile.decrementIORBCount();

                if(openFile.closePending && openFile.getIORBCount() == 0)
                    openFile.close();

                ((GenericList)this.iorbQueue).remove(iorb);
            }
        }
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
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
