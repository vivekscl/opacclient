/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.DrawerLayout;
import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.NavigationAdapter.Item;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;

public abstract class OpacActivity extends Activity {
	protected OpacClient app;
	protected AlertDialog adialog;
	protected AccountDataSource aData;
	
	private int selectedItemPos;
	
	private NavigationAdapter navAdapter;
	private ListView drawerList;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private CharSequence mTitle;
	
	private List<Account> accounts;
	
	protected Fragment fragment;
	protected boolean hasDrawer = false;

	public OpacClient getOpacApplication() {
		return app;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(getContentView());
		app = (OpacClient) getApplication();
		
		aData = new AccountDataSource(this);
		setupDrawer();

	}

	protected abstract int getContentView();

	private void setupDrawer() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if(drawerLayout != null) {
			hasDrawer = true;
			drawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                drawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
	                R.string.drawer_open,  /* "open drawer" description */
	                R.string.drawer_close  /* "close drawer" description */
	                ) {
	
	            /** Called when a drawer has settled in a completely closed state. */
	            public void onDrawerClosed(View view) {
	                super.onDrawerClosed(view);
	                getSupportActionBar().setTitle(mTitle);
	            }
	
	            /** Called when a drawer has settled in a completely open state. */
	            public void onDrawerOpened(View drawerView) {
	                super.onDrawerOpened(drawerView);
	                getSupportActionBar().setTitle(app.getResources().getString(R.string.app_name));
	                if (getCurrentFocus() != null) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(getCurrentFocus()
								.getWindowToken(), 0);
					}
	            }
	        };
	
	        // Set the drawer toggle as the DrawerListener
	        drawerLayout.setDrawerListener(drawerToggle);
	        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	        getSupportActionBar().setHomeButtonEnabled(true);
			
			drawerList = (ListView) findViewById(R.id.drawer_list);
			navAdapter = new NavigationAdapter(this);
			drawerList.setAdapter(navAdapter);
			navAdapter.addSeperatorItem(getString(R.string.nav_hl_library));
			navAdapter.addTextItemWithIcon(getString(R.string.nav_search), R.drawable.ic_action_search);
			navAdapter.addTextItemWithIcon(getString(R.string.nav_account), R.drawable.ic_action_account);
			navAdapter.addTextItemWithIcon(getString(R.string.nav_starred), R.drawable.ic_action_star_1);
			navAdapter.addTextItemWithIcon(getString(R.string.nav_info), R.drawable.ic_action_info);
			
			aData.open();
			accounts = aData.getAllAccounts();
			if (accounts.size() > 1) {	
				navAdapter.addSeperatorItem(getString(R.string.nav_hl_accountlist));
				
				long tolerance = Long.decode(sp.getString("notification_warning",
						"367200000"));
				
				for (final Account account : accounts) {
					Library library;
					try {
						library = ((OpacClient) getApplication())
								.getLibrary(account.getLibrary());
						int expiring = aData.getExpiring(account, tolerance);
						String expiringText = "";
						if (expiring > 0) {
							expiringText = String.valueOf(expiring);
						}
						navAdapter.addLibraryItem(account.getLabel(), library.getCity(), expiringText);
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				selectItem(5); //selects first account
			}
			
			navAdapter.addSeperatorItem(getString(R.string.nav_hl_other));
			navAdapter.addTextItemWithIcon(getString(R.string.nav_settings), R.drawable.ic_action_settings);
			navAdapter.addTextItemWithIcon(getString(R.string.nav_about), R.drawable.ic_action_help);
			
			drawerList.setOnItemClickListener(new DrawerItemClickListener());		
			
			if (!sp.getBoolean("version2.0.0-introduced", false)
					&& app.getSlidingMenuEnabled()) {
				final Handler handler = new Handler();
				// Just show the menu to explain that is there if people start
				// version 2 for the first time.
				// We need a handler because if we just put this in onCreate nothing
				// happens. I don't have any idea, why.
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						SharedPreferences sp = PreferenceManager
								.getDefaultSharedPreferences(OpacActivity.this);
						drawerLayout.openDrawer(drawerList);
						sp.edit().putBoolean("version2.0.0-introduced", true)
								.commit();
					}
				}, 500);
	
			}
		}
	}
	
	private class DrawerItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			selectItem(position);
		}		
	}
	
	 @Override
	    protected void onPostCreate(Bundle savedInstanceState) {
	        super.onPostCreate(savedInstanceState);
	        if(hasDrawer) drawerToggle.syncState();
	    }

	    @Override
	    public void onConfigurationChanged(Configuration newConfig) {
	        super.onConfigurationChanged(newConfig);
	        if(hasDrawer) drawerToggle.onConfigurationChanged(newConfig);
	    }
	
	/** Swaps fragments in the main content view */
	protected void selectItem(int position) {
	    final int count = navAdapter.getCount();
	    if (navAdapter.getItemViewType(position) == Item.TYPE_SEPARATOR) {
	    	//clicked on a separator
	    	return;
	    } else if (navAdapter.getItemViewType(position) == Item.TYPE_TEXT) {
	    	switch (position) {
	    		case 1: fragment = new SearchFragment();
	    				break;
	    		case 2: //fragment = new AccountFragment();
	    				break;
	    		case 3: //fragment = new StarredFragment();
    				break;
	    		case 4: //fragment = new InfoFragment();
    				break;
	    	}
			if(position == count - 2) {
				Intent intent = new Intent(this, MainPreferenceActivity.class);
				startActivity(intent);
				return;
			} else if (position == count - 1) {
				//fragment = new AboutFragment();
			}
			

		    // Insert the fragment by replacing any existing fragment
		    FragmentManager fragmentManager = getSupportFragmentManager();
		    fragmentManager.beginTransaction()
		                   .replace(R.id.content_frame, fragment)
		                   .commit();

		    // Highlight the selected item, update the title, and close the drawer
		    deselectNavItems();
		    drawerList.setItemChecked(position, true);
		    drawerList.setItemChecked(selectedItemPos, false);
		    selectedItemPos = position;
		    setTitle(navAdapter.getItem(position).text);
		    drawerLayout.closeDrawer(drawerList);
			
		} else if (navAdapter.getItemViewType(position) == Item.TYPE_LIBRARY) {
			deselectLibraryItems();
			drawerList.setItemChecked(position, true);
			selectaccount(accounts.get(position-6).getId());
			return;
		}
	}
	
	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		mTitle = title;
	}

	private void deselectLibraryItems() {
		for(int i = 6; i < drawerList.getCount() - 2; i++) {
			drawerList.setItemChecked(i, false);
		}
	}
	
	private void deselectNavItems() {
		for(int i = 1; i < 5; i++) {
			drawerList.setItemChecked(i, false);
		}
		for(int i = drawerList.getCount() - 2; i < drawerList.getCount(); i++) {
			drawerList.setItemChecked(i, false);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (app.getAccount() == null || app.getLibrary() == null) {
			if (!sp.getString("opac_bib", "").equals("")) {
				// Migrate
				Map<String, String> renamed_libs = new HashMap<String, String>();
				renamed_libs.put("Trier (Palais Walderdorff)", "Trier");
				renamed_libs.put("Ludwigshafen (Rhein)", "Ludwigshafen Rhein");
				renamed_libs.put("Neu-Ulm", "NeuUlm");
				renamed_libs.put("Hann. Münden", "HannMünden");
				renamed_libs.put("Münster", "Munster");
				renamed_libs.put("Tübingen", "Tubingen");
				renamed_libs.put("Göttingen", "Gottingen");
				renamed_libs.put("Schwäbisch Hall", "Schwabisch Hall");

				StarDataSource stardata = new StarDataSource(this);
				stardata.renameLibraries(renamed_libs);

				Library lib = null;
				try {
					if (renamed_libs.containsKey(sp.getString("opac_bib", "")))
						lib = app.getLibrary(renamed_libs.get(sp.getString(
								"opac_bib", "")));
					else
						lib = app.getLibrary(sp.getString("opac_bib", ""));
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (lib != null) {
					AccountDataSource data = new AccountDataSource(this);
					data.open();
					Account acc = new Account();
					acc.setLibrary(lib.getIdent());
					acc.setLabel(getString(R.string.default_account_name));
					if (!sp.getString("opac_usernr", "").equals("")) {
						acc.setName(sp.getString("opac_usernr", ""));
						acc.setPassword(sp.getString("opac_password", ""));
					}
					long insertedid = data.addAccount(acc);
					data.close();
					app.setAccount(insertedid);

					Toast.makeText(
							this,
							"Neue Version! Alte Accountdaten wurden wiederhergestellt.",
							Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(
							this,
							"Neue Version! Wiederherstellung alter Zugangsdaten ist fehlgeschlagen.",
							Toast.LENGTH_LONG).show();
				}
			}
		}
		if (app.getLibrary() == null) {
			// Create new
			if (app.getAccount() != null) {
				try {
					InputStream stream = getAssets().open(
							OpacClient.ASSETS_BIBSDIR + "/"
									+ app.getAccount().getLibrary() + ".json");
					stream.close();
				} catch (IOException e) {
					AccountDataSource data = new AccountDataSource(this);
					data.open();
					data.remove(app.getAccount());
					List<Account> available_accounts = data.getAllAccounts();
					if (available_accounts.size() > 0) {
						((OpacClient) getApplication())
								.setAccount(available_accounts.get(0).getId());
					}
					data.close();
					if (app.getLibrary() != null)
						return;
				}
			}
			app.addFirstAccount(this);
			return;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
//		showContent();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_opac, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public interface AccountSelectedListener {
		void accountSelected(Account account);
	}

	public class MetaAdapter extends ArrayAdapter<ContentValues> {

		private List<ContentValues> objects;
		private int spinneritem;

		@Override
		public View getDropDownView(int position, View contentView,
				ViewGroup viewGroup) {
			View view = null;

			if (objects.get(position) == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater
						.inflate(R.layout.simple_spinner_dropdown_item,
								viewGroup, false);
				return view;
			}

			ContentValues item = objects.get(position);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater
						.inflate(R.layout.simple_spinner_dropdown_item,
								viewGroup, false);
			} else {
				view = contentView;
			}

			TextView tvText = (TextView) view.findViewById(android.R.id.text1);
			tvText.setText(item.getAsString("value"));
			return view;
		}

		@Override
		public View getView(int position, View contentView, ViewGroup viewGroup) {
			View view = null;

			if (objects.get(position) == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(spinneritem, viewGroup, false);
				return view;
			}

			ContentValues item = objects.get(position);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(spinneritem, viewGroup, false);
			} else {
				view = contentView;
			}

			TextView tvText = (TextView) view.findViewById(android.R.id.text1);
			tvText.setText(item.getAsString("value"));
			return view;
		}

		public MetaAdapter(Context context, List<ContentValues> objects,
				int spinneritem) {
			super(context, R.layout.simple_spinner_item, objects);
			this.objects = objects;
			this.spinneritem = spinneritem;
		}

	}

	public void accountSelected() {

	}

	public void selectaccount() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Get the layout inflater
		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.simple_list_dialog, null);

		ListView lv = (ListView) view.findViewById(R.id.lvBibs);
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		final List<Account> accounts = data.getAllAccounts();
		data.close();
		AccountListAdapter adapter = new AccountListAdapter(this, accounts);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				app.setAccount(accounts.get(position).getId());

				adialog.dismiss();

				((AccountSelectedListener) fragment).accountSelected(accounts.get(position));
			}
		});
		builder.setTitle(R.string.account_select)
				.setView(view)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						})
				.setNeutralButton(R.string.accounts_edit,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
//TODO:								Intent intent = new Intent(OpacActivity.this,
//										AccountListActivity.class);
//								startActivity(intent);
							}
						});
		adialog = builder.create();
		adialog.show();
	}
	
	public void selectaccount(long id) {
		((OpacClient) getApplication()).setAccount(id);
		accountSelected();
	}

	protected static void unbindDrawables(View view) {
		if (view == null)
			return;
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			if (!(view instanceof AdapterView)) {
				((ViewGroup) view).removeAllViews();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (hasDrawer && drawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        
        return super.onOptionsItemSelected(item);
	}
}
