package com.systemtap.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.systemtap.android.logging.Eventlog;
import com.systemtap.android.net.protocol.SystemTapMessage.ModuleStatus;
import com.systemtap.android.service.Module;
import com.systemtap.android.service.SystemTapBinder;
import com.systemtap.android.service.SystemTapService;

public class SystemTapActivity  extends SherlockFragmentActivity implements ActionBar.TabListener, OnClickListener, Observer, OnItemSelectedListener 
{
	public static final String MODULE_ID = "moduleid";
	public static final String DELETE_ALL_ID = "deleteallid";
	public static final String FILE_ID = "fileid";
	public static final String LOGFILE_OBJECT = "logfileobject";
	public static final String LOGFILE_TAB_ID = "logtab";
	public static final String OUTPUTFILE_TAB_ID = "outputtab";
	public static final String MODULE_TAB_ID = "moduletab";
	public static final String DISMISS_FILE_DETAILS_ID = "dismissfiledetails";
	
	public static final int MODULE_DETAILS_DIALOG = 0x1;
	public static final int FILE_DETAILS_DIALOG = 0x2;
	public static final int DELETE_LOG_FILE_DIALOGUE = 0x4;
	public static final int DELETE_OUTPUT_FILE_DIALOGUE = 0x8;
	public static final int DELETE_MODULE_DIALOGUE = 0x10;
	
	private static final String TAG = SystemTapActivity.class.getSimpleName();
	
	private ModulesOverviewFragment mModulesOverviewFragement;
	private FilesOverviewFragment mFilesOverviewFragment;
	private ReentrantLock mMutex;
    private SystemTapService mSystemTapService;
    
    private Button mButtonModuleDetailsOK;
    private Button mButtonModuleDetailsCtrl;
    private Button mButtonLogFileDetailsOK;
    private Button mButtonLogFileDetailsDelete;
    private TextView mTextViewModuleDetailsName;
    private TextView mTextViewModuleDetailsStatus;
    private TextView mTextViewFileContentsContent;
    private TextView mTextViewFileContentsName;
    private TextView mTextViewFileContentsDate;
    private TextView mTextViewFileContentsTime;
    private Spinner mSpinnerModuleDetailsAction;
    private ArrayAdapter<String> mSpinnerAdapter;
	private int mButtonModuleDetailsOKID;
	private int mButtonModuleDetailsCtrlID;
	private int mButtonLogFileDetailsOKID;
	private int mButtonLogFileDetailsDeleteID;
	private String mSelectedModule;
	private File mFileContentsDialogueFile;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(android.os.Build.VERSION.SDK_INT < 11) {
        	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
		this.setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		this.setContentView(R.layout.activity_systemtap);
		this.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mMutex = new ReentrantLock();
        mModulesOverviewFragement = null;
        mFilesOverviewFragment = null;

	    /* Add the module overview tab */
        ActionBar.Tab tab = this.getSupportActionBar().newTab();
        tab.setText(this.getText(R.string.stap_module));
        tab.setTabListener(this);
        tab.setTag(SystemTapActivity.MODULE_TAB_ID);
        this.getSupportActionBar().addTab(tab);
        
        /* Add the logfile overview tab */
        tab = this.getSupportActionBar().newTab();
        tab.setText(this.getText(R.string.stap_logfiles));
        tab.setTabListener(this);
        tab.setTag(SystemTapActivity.LOGFILE_TAB_ID);
        this.getSupportActionBar().addTab(tab);
        
        /* Add the outputfile overview tab */
        tab = this.getSupportActionBar().newTab();
        tab.setText(this.getText(R.string.stap_outputfiles));
        tab.setTabListener(this);
        tab.setTag(SystemTapActivity.OUTPUTFILE_TAB_ID);
        this.getSupportActionBar().addTab(tab);

        mSelectedModule = null;
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
			String selectedAction = (String)mSpinnerModuleDetailsAction.getSelectedItem();
			if (selectedAction == null) {
				Eventlog.e(TAG, "onClick(): selectedAction is null");
				return;
			}
			if (selectedAction.equalsIgnoreCase(this.getString(R.string.stap_module_details_action_stop))) {
				if (module.getStatus() == ModuleStatus.RUNNING) {
					mSystemTapService.stopModule(module.getName());
				} else {
					Eventlog.e(TAG, "onClick(): User wants to stop module \"" + module.getName() + "\", but module is not running.");
				}
			} else if (selectedAction.equalsIgnoreCase(this.getString(R.string.stap_module_details_action_start))) {
				if (module.getStatus() == ModuleStatus.STOPPED || module.getStatus() == ModuleStatus.CRASHED) {
					mSystemTapService.startModule(module.getName());
				} else {
					Eventlog.e(TAG, "onClick(): User wants to start module \"" + module.getName() + "\", but module is running.");
				}
			} else if (selectedAction.equalsIgnoreCase(this.getString(R.string.stap_module_details_action_delete))) {
				this.showDialog(SystemTapActivity.DELETE_MODULE_DIALOGUE);
				this.dismissDialog(SystemTapActivity.MODULE_DETAILS_DIALOG);
			}
		}
		else if (pView.getId() == mButtonModuleDetailsOKID)
		{
			mSelectedModule = null;
			this.dismissDialog(SystemTapActivity.MODULE_DETAILS_DIALOG);
		}
		else if (pView.getId() == mButtonLogFileDetailsOKID)
		{
			this.dismissDialog(SystemTapActivity.FILE_DETAILS_DIALOG);
			mFileContentsDialogueFile = null;
		}
		else if (pView.getId() == mButtonLogFileDetailsDeleteID) {
        	Bundle args = new Bundle();
        	args.putBoolean(SystemTapActivity.DELETE_ALL_ID, false);
        	args.putBoolean(SystemTapActivity.DISMISS_FILE_DETAILS_ID, true);
        	args.putString(SystemTapActivity.FILE_ID, mFileContentsDialogueFile.getName());

			String tag = (String)this.getSupportActionBar().getSelectedTab().getTag();
			if (tag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID)) {
            	this.showDialog(SystemTapActivity.DELETE_LOG_FILE_DIALOGUE, args);
			} else if (tag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
            	this.showDialog(SystemTapActivity.DELETE_OUTPUT_FILE_DIALOGUE, args);
			}
		}
	}
	
	@Override
	public Dialog onCreateDialog(int pID, Bundle pArgs)
	{
		Dialog dialogue = null;
		AlertDialog.Builder builder = null;
		final Bundle args = pArgs;
		
		switch(pID)
		{
			case MODULE_DETAILS_DIALOG:
				dialogue = new Dialog(this);
				dialogue.setOwnerActivity(this);
	            dialogue.requestWindowFeature(Window.FEATURE_NO_TITLE);
	            dialogue.setContentView(R.layout.module_details);
	            dialogue.setCancelable(false);
	            
	            mButtonModuleDetailsOK = (Button)dialogue.findViewById(R.id.buttonModuleDetailsOk);
	            mButtonModuleDetailsOK.setOnClickListener(this);
	            mButtonModuleDetailsOKID = mButtonModuleDetailsOK.getId();
	            
	            mButtonModuleDetailsCtrl = (Button)dialogue.findViewById(R.id.buttonModuleDetailsCtl);
	            mButtonModuleDetailsCtrl.setOnClickListener(this);
	            mButtonModuleDetailsCtrlID = mButtonModuleDetailsCtrl.getId();
	            
	            mTextViewModuleDetailsName = (TextView)dialogue.findViewById(R.id.textViewModuleDetailsNameValue);
	            mTextViewModuleDetailsStatus = (TextView)dialogue.findViewById(R.id.textViewModuleDetailsStatusValue);
	            mSpinnerModuleDetailsAction = (Spinner)dialogue.findViewById(R.id.spinnerModuleDetailsActionValue);
	            mSpinnerAdapter = new ArrayAdapter<String>(this.getSupportActionBar().getThemedContext(),com.actionbarsherlock.R.layout.sherlock_spinner_item);
	            mSpinnerAdapter.setDropDownViewResource(com.actionbarsherlock.R.layout.sherlock_spinner_dropdown_item);
	            mSpinnerModuleDetailsAction.setAdapter(mSpinnerAdapter);
	            mSpinnerModuleDetailsAction.setOnItemSelectedListener(this);
				break;
			
			case FILE_DETAILS_DIALOG:
				dialogue = new Dialog(this);
				dialogue.setOwnerActivity(this);
	            dialogue.requestWindowFeature(Window.FEATURE_NO_TITLE);
	            dialogue.setContentView(R.layout.file_details);
	            dialogue.setCancelable(false);
	            
	            mButtonLogFileDetailsOK = (Button)dialogue.findViewById(R.id.buttonFileDetailsOk);
	            mButtonLogFileDetailsOK.setOnClickListener(this);
	            mButtonLogFileDetailsOKID = mButtonLogFileDetailsOK.getId();

	            mButtonLogFileDetailsDelete = (Button)dialogue.findViewById(R.id.buttonFileDetailsDelete);
	            mButtonLogFileDetailsDelete.setOnClickListener(this);
	            mButtonLogFileDetailsDeleteID = mButtonLogFileDetailsDelete.getId();
	            
	            mTextViewFileContentsContent = (TextView)dialogue.findViewById(R.id.textViewFileDetailsContent);
	            mTextViewFileContentsName = (TextView)dialogue.findViewById(R.id.textViewFileContentsNameValue);
	            mTextViewFileContentsDate = (TextView)dialogue.findViewById(R.id.textViewFileContentsDateValue);
	            mTextViewFileContentsTime = (TextView)dialogue.findViewById(R.id.textViewFileContentsTimeValue);
				break;
				
			case DELETE_LOG_FILE_DIALOGUE:
				builder = new AlertDialog.Builder(this);
				if (pArgs.getBoolean(SystemTapActivity.DELETE_ALL_ID)) {
					builder.setMessage(R.string.stap_dialogue_delete_all_log_files);
				} else {
					builder.setMessage(R.string.stap_dialogue_delete_log_file);
				}
				builder.setTitle(R.string.stap_dialogue_delete_file_heading)
				/*
				 * Create dummy buttons and message. The builder will create these view elements in the dialog.
				 * Although they are not filled right now, it necessary to create them. Otherwise a setMessage() in onPrepareDialog() will have no effect.
				 */
				.setMessage("")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
				    }
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
				    }
				});
				dialogue = builder.create();
				break;
				
			case DELETE_OUTPUT_FILE_DIALOGUE:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.stap_dialogue_delete_file_heading)
				/*
				 * Create dummy buttons and message. The builder will create these view elements in the dialog.
				 * Although they are not filled right now, it necessary to create them. Otherwise a setMessage() in onPrepareDialog() will have no effect.
				 */
				.setMessage("")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
				    }
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
				    }
				});
				dialogue = builder.create();
				break;

			case DELETE_MODULE_DIALOGUE:	
				builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.stap_dialogue_delete_module_heading)
				.setMessage(R.string.stap_dialogue_delete_module)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface pDialog, int pID) {
				    	if (mSystemTapService != null) {
				    		mSystemTapService.deleteModule(mSelectedModule);
				        	Bundle args = new Bundle();
				        	args.putBoolean(SystemTapActivity.DELETE_ALL_ID, true);
				        	args.putString(SystemTapActivity.MODULE_ID, mSelectedModule);
				        	SystemTapActivity.this.showDialog(SystemTapActivity.DELETE_LOG_FILE_DIALOGUE, args);
				        	SystemTapActivity.this.showDialog(SystemTapActivity.DELETE_OUTPUT_FILE_DIALOGUE, args);
				    	}
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
				    }
				});
				dialogue = builder.create();
				break;

			default:
				dialogue = null;
		}
		return dialogue;
	}
	
	@Override
	public void onPrepareDialog(int pID, Dialog pDialog, Bundle pArgs)
	{
		String moduleName = null;
		AlertDialog alertDialog = null;
		final Bundle args = pArgs;

		switch(pID)
		{
			case MODULE_DETAILS_DIALOG:
				moduleName = pArgs.getString(SystemTapActivity.MODULE_ID);
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
				this.updateModuleDetailsDialogue(module);
				break;
				
			case FILE_DETAILS_DIALOG:
				mFileContentsDialogueFile = (File)pArgs.getSerializable(SystemTapActivity.LOGFILE_OBJECT);
				final File file = mFileContentsDialogueFile;
				moduleName = pArgs.getString(SystemTapActivity.MODULE_ID);
				if (file == null)
				{
					Eventlog.e(TAG,"file not given");
					break;
				}
				/* Get file name */
				String fileName = file.getName();
				/* Strip off the extension */
				fileName = fileName.substring(0,fileName.lastIndexOf('.'));
				int dateEndPos = fileName.lastIndexOf('_') - 1;
				/* extract the date */
				String date = fileName.substring(fileName.lastIndexOf('_',dateEndPos) + 1,dateEndPos + 1);
				/* extract the time */
				String time = fileName.substring(fileName.lastIndexOf('_') + 1);
				time = time.replace(".", ":");
				mTextViewFileContentsName.setText(moduleName);
				mTextViewFileContentsDate.setText(date);
				mTextViewFileContentsTime.setText(time);
				if (!file.exists())
				{
					mTextViewFileContentsContent.setText(R.string.stap_logfile_details_filenotfound);
					break;
				}
				mTextViewFileContentsContent.setText("");
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
								mTextViewFileContentsContent.append(buff);
							}
							in.close();
						}
						catch (IOException e)
						{
							Eventlog.printStackTrace(TAG, e);
						}
					}
				});
				break;

			case DELETE_OUTPUT_FILE_DIALOGUE:
				alertDialog = (AlertDialog)pDialog;
				if (pArgs.getBoolean(SystemTapActivity.DELETE_ALL_ID)) {
					alertDialog.setMessage(this.getText(R.string.stap_dialogue_delete_all_output_files));
				} else {
					alertDialog.setMessage(this.getText(R.string.stap_dialogue_delete_output_file));
				}
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,this.getText(android.R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
		            	if (SystemTapActivity.this.mSystemTapService != null) {
		            		// If the dialog was triggered by the delete module dialog, the module name will be supplied via Bundle
		            		String moduleName = args.getString(SystemTapActivity.MODULE_ID);
		            		if (moduleName == null) {
		            			moduleName = mFilesOverviewFragment.getSelectedModule();
		            		}
		            		if (args.getBoolean(SystemTapActivity.DELETE_ALL_ID)) {
		            			SystemTapActivity.this.mSystemTapService.deleteAllOutputFiles(moduleName);
		    				} else {
		    					SystemTapActivity.this.mSystemTapService.deleteOutputFile(moduleName,args.getString(SystemTapActivity.FILE_ID));
		    				}
		            		/* If the dialog was triggered by the delete module dialog, the module name will be supplied via Bundle
		            		 * Otherwise the module name is retrieved via the getSelectedModule(). Thus a file details dialog is still shown.
		            		 */
		            		if (args.getBoolean(SystemTapActivity.DISMISS_FILE_DETAILS_ID)) {
		            			SystemTapActivity.this.dismissDialog(SystemTapActivity.FILE_DETAILS_DIALOG);
		            		}
		            		/*
		            		 * Same here. No module id was supplied. Hence, this dialog was triggered from the log or output tab.
		            		 * A refresh is required.
		            		 */
		            		if (args.getString(SystemTapActivity.MODULE_ID) == null) {
		            			mFilesOverviewFragment.refreshFileList();
		            		}
		            	}
					}
				});
				break;

			case DELETE_LOG_FILE_DIALOGUE:
				alertDialog = (AlertDialog)pDialog;
				if (pArgs.getBoolean(SystemTapActivity.DELETE_ALL_ID)) {
					alertDialog.setMessage(this.getText(R.string.stap_dialogue_delete_all_log_files));
				} else {
					alertDialog.setMessage(this.getText(R.string.stap_dialogue_delete_log_file));
				}
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,this.getText(android.R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
		            	if (SystemTapActivity.this.mSystemTapService != null) {
		            		String moduleName = args.getString(SystemTapActivity.MODULE_ID);
		            		if (moduleName == null) {
		            			moduleName = mFilesOverviewFragment.getSelectedModule();
		            			if (moduleName == null) {
		            				return;
		            			}
		            		}
		            		if (args.getBoolean(SystemTapActivity.DELETE_ALL_ID)) {
		            			SystemTapActivity.this.mSystemTapService.deleteAllLogFiles(moduleName);
		    				} else {
		    					SystemTapActivity.this.mSystemTapService.deleteLogFile(moduleName,args.getString(SystemTapActivity.FILE_ID));
		    				}
		            		/* If the dialog was triggered by the delete module dialog, the module name will be supplied via Bundle
		            		 * Otherwise the module name is retrieved via the getSelectedModule(). Thus a file details dialog is still shown.
		            		 */
		            		if (args.getBoolean(SystemTapActivity.DISMISS_FILE_DETAILS_ID)) {
		            			SystemTapActivity.this.dismissDialog(SystemTapActivity.FILE_DETAILS_DIALOG);
		            		}
		            		/*
		            		 * Same here. No module id was supplied. Hence, this dialog was triggered from the log or output tab.
		            		 * A refresh is required.
		            		 */
		            		if (args.getString(SystemTapActivity.MODULE_ID) == null) {
		            			mFilesOverviewFragment.refreshFileList();
		            		}
		            	}
					}
				});
				break;
			
			default:
				super.onPrepareDialog(pID, pDialog,pArgs);
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu pMenu)
    {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_menu,pMenu);
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu pMenu)
	{
        ActionBar actionBar = this.getSupportActionBar();
        String tag = (String)actionBar.getSelectedTab().getTag();
        boolean visible = tag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID) || tag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID);
        
        MenuItem item = pMenu.findItem(R.id.menuItemRefresh);
        item.setVisible(visible);
        item = pMenu.findItem(R.id.menuItemDeleteAll);
        item.setVisible(visible);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        switch (item.getItemId())
        {
            case R.id.menuItemExit:
            	intent = new Intent(this,SystemTapService.class);
            	if (mSystemTapService != null)
            	{
            		this.unbindService(mConnection);
            		Eventlog.d(TAG,"SystemTapService unbounded");
                	mSystemTapService.unregisterObserver(this);
            		mSystemTapService = null;
            		this.stopService(intent);
            		this.finish();
            	}
            	return true;
            case R.id.menuItemRefresh:
        		Eventlog.d(TAG, "Refreshing file list");
        		mFilesOverviewFragment.refreshFileList();
            	return true;
            case R.id.menuItemSettings:
            	intent = new Intent(this, SettingsActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    			this.startActivity(intent);
            	return true;
            case R.id.menuItemDeleteAll:
            	Bundle args = new Bundle();
            	args.putBoolean(SystemTapActivity.DELETE_ALL_ID, true);
            	args.putBoolean(SystemTapActivity.DISMISS_FILE_DETAILS_ID, false);
            	String tag = (String)this.getSupportActionBar().getSelectedTab().getTag();
				if (tag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID)) {
	            	this.showDialog(SystemTapActivity.DELETE_LOG_FILE_DIALOGUE, args);
				} else if (tag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
	            	this.showDialog(SystemTapActivity.DELETE_OUTPUT_FILE_DIALOGUE, args);	
				}
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
					SystemTapActivity.this.updateModuleDetailsDialogue(module);
				}
				else
				{
					Eventlog.d(TAG, "update(): mSelectedModule is null");
				}
			}
		});
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

    private void updateModuleDetailsDialogue(Module pModule) {
		mTextViewModuleDetailsName.setText(pModule.getName());
		mSpinnerAdapter.clear();
		switch(pModule.getStatus())
		{
			case RUNNING:
				mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_running));
				mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_running));
				mSpinnerAdapter.add(this.getString(R.string.stap_module_details_action_stop));
				break;

			case STOPPED:
				mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_stopped));
				mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_stopped));
				mSpinnerAdapter.add(this.getString(R.string.stap_module_details_action_start));
				break;

			case CRASHED:
				mTextViewModuleDetailsStatus.setText(this.getText(R.string.stap_module_crashed));
				mTextViewModuleDetailsStatus.setTextColor(this.getResources().getColor(R.color.stap_module_crashed));
				mSpinnerAdapter.add(this.getString(R.string.stap_module_details_action_start));
				break;
		}
		mSpinnerAdapter.add(this.getString(R.string.stap_module_details_action_delete));
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

    @Override
    public void onTabReselected(Tab pTab, FragmentTransaction pTransaction)
    {
    
    }

	@Override
	public void onTabSelected(Tab pTab, FragmentTransaction pTransaction)
	{
		if (pTab == null || pTransaction == null) {
			Eventlog.e(TAG,"tab null");
			return;
		}
		String tag = (String)pTab.getTag();
		if (tag.equalsIgnoreCase(SystemTapActivity.MODULE_TAB_ID)) {
			if (mModulesOverviewFragement == null) {
				mModulesOverviewFragement = (ModulesOverviewFragment) Fragment.instantiate(this, ModulesOverviewFragment.class.getName());
				pTransaction.add(R.id.frameLayoutContentContainer,mModulesOverviewFragement);
			} else {
				pTransaction.attach(mModulesOverviewFragement);
			}
		} else if (tag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID) || tag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
			if (mFilesOverviewFragment == null) {
				mFilesOverviewFragment = (FilesOverviewFragment) Fragment.instantiate(this, FilesOverviewFragment.class.getName());
				pTransaction.add(R.id.frameLayoutContentContainer,mFilesOverviewFragment);
			} else {
				pTransaction.attach(mFilesOverviewFragment);
			}
		}
	}
       
	@Override
	public void onTabUnselected(Tab pTab, FragmentTransaction pTransaction)
	{
		String tag = (String)pTab.getTag();
		if (tag.equalsIgnoreCase(SystemTapActivity.MODULE_TAB_ID)) {
			pTransaction.detach(mModulesOverviewFragement);
		} else if (tag.equalsIgnoreCase(SystemTapActivity.LOGFILE_TAB_ID) || tag.equalsIgnoreCase(SystemTapActivity.OUTPUTFILE_TAB_ID)) {
			pTransaction.detach(mFilesOverviewFragment);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> pParent, View pView, int pPosition, long pID) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> pParent) {		
	}

}
