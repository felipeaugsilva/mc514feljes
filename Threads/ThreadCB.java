/*
* Grupo 05
* RA: 081704
* RA: 096993
*
* Status:
*
* 09/09/2010
* 1. Criação da do_create(não foi testada ainda)
* 2. Começo da criação da do_kill.

*16/09/2010
*1. Término da do_kill
*2. Criação do do_suspend e do_resume

*23/09/2010
*1. Inicio do_dispatch();
*2. Há erros de compilação.
* */ 


package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{

    private GenericList readyQueue;

    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
		super();
      	readyQueue = new GenericList();
		readyQueue.insert(this);
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
		if (task.getThreadCount() == MaxThreadsPerTask)
			return null;

		ThreadCB thread = new ThreadCB();

        if (task.addThread(thread) == FAILURE)
			return null;
         
        thread.setTask(task);
        thread.setPriority(0);
        thread.setStatus(ThreadReady);
        thread.dispatch();       

        return thread;    
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
		TaskCB task = this.getTask(); 
		int status = this.getStatus();

		this.setStatus(ThreadKill);

		task.removeThread(this);

		switch(status) {

			case ThreadReady:
				readyQueue.remove(this);
			break;

			case ThreadWaiting:
				for(int i = 0; i <= Device.getTableSize(); i++) Device.get(i).cancelPendingIO(this);
			break;

			case ThreadRunning:
				if(task.getThreadCount() == 0)
					task.kill();
				else
					ThreadCB.dispatch();
				break;
       }

		ResourceCB.giveupResources(this);

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        int status = this.getStatus();
		TaskCB task = MMU.getPTBR().getTask();

		if(status == ThreadRunning) {
			this.setStatus(ThreadWaiting);
			MMU.setPTBR(null);
			task.setCurrentThread(null);
			ThreadCB.dispatch();
		}
		else
			this.setStatus(status+1);

		if(!event.contains(this))
			event.addThread(this);

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        int status = this.getStatus();

		switch(status){
			case ThreadWaiting: this.setStatus(ThreadReady);
								readyQueue.insert(this);
								break;
			default: this.setStatus(status-1);
					 break;
		}

		ThreadCB.dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
		TaskCB task = MMU.getPTBR().getTask();
		ThreadCB thread = task.getCurrentThread();
		ThreadCB newThread;

		if(readyQueue.isEmpty()) {
			if(thread == null)
				return FAILURE;
			return SUCCESS;
		}
		if(thread != null) {
			runningThread.setStatus(ThreadReady);
			MMU.setPTBR(null);
			task.setCurrentThread(null);
		}

		newThread = readyQueue.removeTail();
		newThread.setStatus(ThreadRunning);
		MMU.getPTBR().setPTBR(task.getPageTable());
		task.setCurrentThread(newThread);

		return SUCCESS;

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
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