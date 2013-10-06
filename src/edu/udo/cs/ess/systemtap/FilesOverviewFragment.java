package edu.udo.cs.ess.systemtap;

import java.io.File;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.Module;
import edu.udo.cs.ess.systemtap.service.SystemTapBinder;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class FilesOverviewFragment extends SherlockFragment implements OnItemClickListener, OnItemSelectedListener, Observer
{
	private static final String TAG = FilesOverviewFragment.class.getSimpleName();
	
	private SystemTapService mSystemTapService;
	private ReentrantLock mMutex;
	private FileListAdapter mLogFileListAdapter;
	private ListView mListViewModules;
	private int mListViewID;
	private ArrayAdapter<String> mModuleSpinnerAdapter;
	private Spinner mSpinnerModules;
	private int mSpinnerModulesID;
	private String mCurrentTag;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.file_list, container, false);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        mMutex = new ReentrantLock();
		this.getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		
        mLogFileListAdapter = new FileListAdapter(this.getSherlockActivity().getSupportActionBar().getThemedContext(),R.layout.file_list_item);
        mListViewModules = (ListView)this.getActivity().findViewById(R.id.listViewFileList);
		mListViewModules.setEmptyView(this.getActivity().findViewById(android.R.id.empty));
		mListViewModules.setAdapter(mLogFileListAdapter);
		mListViewModules.setOnItemClickListener(this);
		mListViewID = mListViewModules.getId();
		
		mModuleSpinnerAdapter = new ArrayAdapter<String>(this.getSherlockActivity().getSupportActionBar().getThemedContext(),com.actionbarsherlock.R.layout.sherlock_spinner_item);
		mModuleSpinnerAdapter.setDropDownViewResource(com.actionbarsherlock.R.layout.sherlock_spinner_dropdown_item);
		mSpinnerModules = (Spinner)this.getActivity().findViewById(R.id.spinnerFileList);
		mSpinnerModules.setAdapter(mModuleSpinnerAdapter);
		mSpinnerModules.setOnItemSelectedListener(this);
		mSpinnerModulesID = mSpinnerModules.getId();
		
		mCurrentTag = null;
	}
	
    @Override
    public void onStart()
    {
    	Intent intent = new Intent(this.getActivity(),SystemTapService.class);
    	this.getActivity().startService(intent);
    	
    	/* Hold the lock, so onStop() will block til the service is bounded */
    	mMutex.lock();
    	this.getActivity().bindService(new Intent(this.getActivity(),SystemTapService.class), mConnection, 0);
    	mCurrentTag = (String)this.getSherlockActivity().getSupportActionBar().getSelectedTab().getTag();
    	
    	super.onStart();
    }

    @Override
    public void onResume()
    {
    	super.onResume();
    }

    @Override
    public void onPause()
    {
    	super.onPause();
    }

    @Override
    public void onStop()
    {
    	/* If the application immediately gets suspend, wait until the service is bounded */
    	mMutex.lock();
    	if (mSystemTapService != null)
    	{
        	mSystemTapService.unregisterObserver(this);
    		this.getActivity().unbindService(mConnection);
    		Eventlog.d(TAG,"SystemTapService unbounded");
    		mSystemTapService = null;
    	}
    	else
    	{
    		Eventlog.e(TAG, "mSystemTapService is null!");
    	}
    	mMutex.unlock();
    	
    	super.onStop();
    }

	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pID)
	{
		if (pParent.getId() == mListViewID)
		{
			File file = (File)mLogFileListAdapter.getItem(pPosition);
			Bundle args = new Bundle();
			/* Pass the selected file as a parameter to the dialog */
			args.putSerializable(SystemTapActivity.LOGFILE_OBJECT, file);
			String name = (String)mSpinnerModules.getSelectedItem();
			args.putString(SystemTapActivity.MODULE_ID, name);
			this.getActivity().showDialog(SystemTapActivity.LOGFILE_DETAILS_DIALOG, args);
		}
		else
		{
			Eventlog.e(TAG,"onClick(): unknown source");
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> pParent, View pView, int pPosition, long pID)
	{
		if (pParent.getId() == mSpinnerModulesID)
		{
			this.refreshFileList();
		}
		else
		{
			Eventlog.e(TAG,"onClick(): unknown source");
		}	
	}

	public void refreshFileList()
	{
		if (mSystemTapService == null)
		{
			Eventlog.e(TAG,"refreshFileList(): mSystemTapService is null");
			return;
		}
		String moduleName = (String)mSpinnerModules.getSelectedItem();
		if (moduleName != null)
		{
			File files[] = null;
			if (mCurrentTag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID)) {
				/* Get all log files for this module and pass them to the underlying adapter */
				files = mSystemTapService.getLogFiles(moduleName);
			} else if (mCurrentTag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
				/* Get all output files for this module and pass them to the underlying adapter */
				files = mSystemTapService.getOutputFiles(moduleName);
			}
			mLogFileListAdapter.clear();
			for (File file : files) {
				mLogFileListAdapter.add(file);
			}
		}
		else
		{
			/* The dummy item was selected, no files to display */
			Eventlog.d(TAG,"Dummy item selected");
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> pParent)
	{
		
	}
    
	@Override
	public void update(Observable pObservable, final Object pData)
	{
		/* Some sanity checks to ensure pData is a Collection */
		if (pData instanceof Collection<?>)
		{
			/* If the adapter gets new data, a call to notifyDataSetChanged() should be done from the ui thread. */
			/* Only the ui thread is allowed to manipulate the gui */
			this.getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Collection<Module> newData = (Collection<Module>)pData;
					mModuleSpinnerAdapter.clear();
		        	for (Module module : newData) {
		        		mModuleSpinnerAdapter.add(module.getName());
		        	}
				}
			});
		}
	}
	
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	FilesOverviewFragment.this.mSystemTapService = ((SystemTapBinder)service).getService();
        	FilesOverviewFragment.this.mModuleSpinnerAdapter.clear();
        	for (Module module : mSystemTapService.getModules()) {
        		FilesOverviewFragment.this.mModuleSpinnerAdapter.add(module.getName());
        	}
        	Object item = FilesOverviewFragment.this.mSpinnerModules.getSelectedItem();
        	if (item != null)
        	{
        		/* Set the ListView to a initial state - if more than 1 module is available, get the selected one and pass the avaible logfiles to the listview-adapter */
        		String moduleName = (String)item;
				File files[] = null;
				if (FilesOverviewFragment.this.mCurrentTag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID)) {
					/* Get all log files for this module and pass them to the underlying adapter */
					files = FilesOverviewFragment.this.mSystemTapService.getLogFiles(moduleName);
				} else if (FilesOverviewFragment.this.mCurrentTag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
					/* Get all output files for this module and pass them to the underlying adapter */
					files = FilesOverviewFragment.this.mSystemTapService.getOutputFiles(moduleName);
				}
				mLogFileListAdapter.clear();
				if(android.os.Build.VERSION.SDK_INT < 11) {
					for (File file : files) {
						mLogFileListAdapter.add(file);
					}
				} else {
					mLogFileListAdapter.addAll(files);
				}
        	}
        	else
        	{
        		Eventlog.e(FilesOverviewFragment.TAG,"onServiceConnected(): No item selected");
        	}
        	/* the ModuleSpinnerAdapter wants to get notified if the set of modules has changed */
        	mSystemTapService.registerObserver(FilesOverviewFragment.this);
        	FilesOverviewFragment.this.mMutex.unlock();
    		Eventlog.d(TAG,"SystemTapService bounded");
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };
}
