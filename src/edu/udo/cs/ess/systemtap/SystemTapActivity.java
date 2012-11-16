package edu.udo.cs.ess.systemtap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.Module;
import edu.udo.cs.ess.systemtap.service.SystemTapBinder;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class SystemTapActivity  extends SherlockFragmentActivity implements ActionBar.TabListener, OnClickListener, Observer 
{
	public static final String CONTEXT_TAG = "context";
	public static final String MODULE_ID = "moduleid";
	public static final String LOGFILE_OBJECT = "logfileobject";
	
	public static final int  MODULE_DETAILS_DIALOG = 0x1;
	public static final int LOGFILE_DETAILS_DIALOG = 0x2;
	
	private static final String TAG = SystemTapActivity.class.getSimpleName();
	
	private ModulesOverviewFragment mModulesOverviewFragement;
	private LogFilesOverviewFragment mLogFilesOverviewFragment;
	private ReentrantLock mMutex;
    private SystemTapService mSystemTapService;
    
    private Button mButtonModuleDetailsOK;
    private Button mButtonModuleDetailsCtrl;
    private Button mButtonLogFileDetailsOK;
    private TextView mTextViewModuleDetailsName;
    private TextView mTextViewModuleDetailsStatus;
    private TextView mTextViewLogFileDetailsContent;
    private TextView mTextViewLogFileDetailsHeading;
	private int mButtonModuleDetailsOKID;
	private int mButtonModuleDetailsCtrlID;
	private int mButtonLogFileDetailsOKID;
	private String mSelectedModule;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar);
		this.setContentView(R.layout.activity_systemtap);
		this.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
        mMutex = new ReentrantLock();
	    
	    /* Add the module overview tab */
        ActionBar.Tab tab = this.getSupportActionBar().newTab();
        tab.setText(this.getText(R.string.stap_module));
        tab.setTabListener(this);
        mModulesOverviewFragement = (ModulesOverviewFragment) Fragment.instantiate(this, ModulesOverviewFragment.class.getName());
        tab.setTag(mModulesOverviewFragement);
        this.getSupportActionBar().addTab(tab);
        
        /* Add the logfile overview tab */
        tab = this.getSupportActionBar().newTab();
        tab.setText(this.getText(R.string.stap_logfiles));
        tab.setTabListener(this);
        mLogFilesOverviewFragment = (LogFilesOverviewFragment) Fragment.instantiate(this, LogFilesOverviewFragment.class.getName());
        tab.setTag(mLogFilesOverviewFragment);
        this.getSupportActionBar().addTab(tab);
        
        mSelectedModule = null;
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction transaction)
	{

	}
	
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction transaction)
	{
		if (tab == null || transaction == null)
		{
			Eventlog.e(TAG,"tab null");
			return;
		}

		transaction.replace(R.id.frameLayoutContentContainer, (Fragment) tab.getTag());
	}
	
	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction transaction)
	{
		
	}
	
	public void onClick(View pView)
	{
		if (pView.getId() == mButtonModuleDetailsCtrlID)
		{
			if (mSelectedModule == null)
			{
				Eventlog.e(TAG, "onClick(): mSelectedModule is null");
				return;
			}
			Module module = mSystemTapService.getModule(mSelectedModule);
			if (module == null)
			{
				Eventlog.e(TAG,"onClick(): module is null");
				return;
			}
			switch (module.getStatus())
			{
				case RUNNING:
					mSystemTapService.stopModule(module.getName());
					break;
					
				case STOPPED:
				case CRASHED:
					mSystemTapService.startModule(module.getName());
					break;
			}
		}
		else if (pView.getId() == mButtonModuleDetailsOKID)
		{
			mSelectedModule = null;
			this.dismissDialog(SystemTapActivity.MODULE_DETAILS_DIALOG);
		}
		else if (pView.getId() == mButtonLogFileDetailsOKID)
		{
			this.dismissDialog(SystemTapActivity.LOGFILE_DETAILS_DIALOG);
		}
	}
	
	@Override
	public Dialog onCreateDialog(int pID, Bundle pArgs)
	{
		Dialog dialog;
		
		switch(pID)
		{
			case MODULE_DETAILS_DIALOG:
				dialog = new Dialog(this);
				dialog.setOwnerActivity(this);
	            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	            dialog.setContentView(R.layout.module_details);
	            dialog.setCancelable(false);
	            
	            mButtonModuleDetailsOK = (Button)dialog.findViewById(R.id.buttonModuleDetailsOk);
	            mButtonModuleDetailsOK.setOnClickListener(this);
	            mButtonModuleDetailsOKID = mButtonModuleDetailsOK.getId();
	            
	            mButtonModuleDetailsCtrl = (Button)dialog.findViewById(R.id.buttonModuleDetailsCtl);
	            mButtonModuleDetailsCtrl.setOnClickListener(this);
	            mButtonModuleDetailsCtrlID = mButtonModuleDetailsCtrl.getId();
	            
	            mTextViewModuleDetailsName = (TextView)dialog.findViewById(R.id.textViewModuleDetailsNameValue);
	            
	            mTextViewModuleDetailsStatus = (TextView)dialog.findViewById(R.id.textViewModuleDetailsStatusValue);
				break;
			
			case LOGFILE_DETAILS_DIALOG:
				dialog = new Dialog(this);
				dialog.setOwnerActivity(this);
	            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	            dialog.setContentView(R.layout.file_details);
	            dialog.setCancelable(false);
	            
	            mButtonLogFileDetailsOK = (Button)dialog.findViewById(R.id.buttonFileDetailsOk);
	            mButtonLogFileDetailsOK.setOnClickListener(this);
	            mButtonLogFileDetailsOKID = mButtonLogFileDetailsOK.getId();
	            
	            mTextViewLogFileDetailsContent = (TextView)dialog.findViewById(R.id.textViewFileDetailsContent);
	            mTextViewLogFileDetailsHeading = (TextView)dialog.findViewById(R.id.textViewFileDetailsHeading);
				break;
				
			default:
				dialog = null;
		}
		return dialog;
	}

	@Override
	public void update(Observable observable, Object data)
	{
		this.runOnUiThread(new Runnable()
		{
			public void run()
			{
				if (SystemTapActivity.this.mSelectedModule != null)
				{
					Module module = SystemTapActivity.this.mSystemTapService.getModule(SystemTapActivity.this.mSelectedModule);
					if (module == null)
					{
						Eventlog.e(TAG,"update(): module is null");
						return;
					}
					switch(module.getStatus())
					{
						case RUNNING:
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setText(SystemTapActivity.this.getText(R.string.stap_module_running));
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setTextColor(SystemTapActivity.this.getResources().getColor(R.color.stap_module_running));
							SystemTapActivity.this.mButtonModuleDetailsCtrl.setText(R.string.stap_button_stop);
							break;
		
						case STOPPED:
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setText(SystemTapActivity.this.getText(R.string.stap_module_stopped));
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setTextColor(SystemTapActivity.this.getResources().getColor(R.color.stap_module_stopped));
							SystemTapActivity.this.mButtonModuleDetailsCtrl.setText(R.string.stap_button_start);
							break;
		
						case CRASHED:
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setText(SystemTapActivity.this.getText(R.string.stap_module_crashed));
							SystemTapActivity.this.mTextViewModuleDetailsStatus.setTextColor(SystemTapActivity.this.getResources().getColor(R.color.stap_module_crashed));
							SystemTapActivity.this.mButtonModuleDetailsCtrl.setText(R.string.stap_button_start);
							break;
					}
				}
				else
				{
					Eventlog.e(TAG, "update(): mSelectedModule is null");
				}
			}
		});
	}
	
	@Override
	public void onPrepareDialog(int pID, Dialog pDialog, Bundle pArgs)
	{
		switch(pID)
		{
			case MODULE_DETAILS_DIALOG:
				String moduleName = pArgs.getString(SystemTapActivity.MODULE_ID);
				if (moduleName == null)
				{
					Eventlog.e(TAG, "onPrepareDialog(): moduleName is null");
					break;
				}
				Module module = this.mSystemTapService.getModule(moduleName);
				if (module == null)
				{
					Eventlog.e(TAG, "onPrepareDialog(): module is null");
					break;
				}
				
				mSelectedModule = moduleName;
				mTextViewModuleDetailsName.setText(module.getName());
				switch(module.getStatus())
				{
					case RUNNING:
						mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_running));
						mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_running));
						mButtonModuleDetailsCtrl.setText(R.string.stap_button_stop);
						break;

					case STOPPED:
						mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_stopped));
						mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_stopped));
						mButtonModuleDetailsCtrl.setText(R.string.stap_button_start);
						break;

					case CRASHED:
						mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_crashed));
						mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_crashed));
						mButtonModuleDetailsCtrl.setText(R.string.stap_button_start);
						break;
				}
				break;
				
			case LOGFILE_DETAILS_DIALOG:
				final File file = (File)pArgs.getSerializable(SystemTapActivity.LOGFILE_OBJECT);
				if (file == null)
				{
					Eventlog.e(TAG,"file not given");
					break;
				}
				mTextViewLogFileDetailsContent.setText("");
				this.runOnUiThread(new Runnable()
				{
					public void run()
					{
						try
						{
							BufferedReader in = new BufferedReader(new FileReader(file));
							String buff;
							while ((buff = in.readLine()) != null)
							{
								buff += "\n";
								mTextViewLogFileDetailsContent.append(buff);
							}
							in.close();
						}
						catch (IOException e)
						{
							Eventlog.printStackTrace(TAG, e);
						}
					}
				});
				mTextViewLogFileDetailsHeading.setText(file.getName());
				break;
			
			default:
				super.onPrepareDialog(pID, pDialog,pArgs);
		}
	}
	
	@Override
    public void onStart()
    {
    	Intent intent = new Intent(this,SystemTapService.class);
    	this.startService(intent);
    	
    	mMutex.lock();
    	this.bindService(new Intent(this,SystemTapService.class), mConnection, 0);

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
    	mMutex.lock();
    	if (mSystemTapService != null)
    	{
    		this.unbindService(mConnection);
    		Eventlog.d(TAG,"SystemTapService unbounded");
        	mSystemTapService.unregisterObserver(this);
    		mSystemTapService = null;
    	}
    	else
    	{
    		Eventlog.e(TAG, "mSystemTapService is null!");
    	}
    	mMutex.unlock();
    	
    	super.onStop();
    }

    private ServiceConnection mConnection = new ServiceConnection()
    {
		public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mSystemTapService = ((SystemTapBinder)service).getService();
        	mSystemTapService.registerObserver(SystemTapActivity.this);
        	SystemTapActivity.this.mMutex.unlock();
    		Eventlog.d(TAG,"SystemTapService bounded");
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };
}
