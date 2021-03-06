package com.st.nyamnew.activities;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.st.nyamnew.NyamApplication;
import com.st.nyamnew.R;
import com.st.nyamnew.activities.DialogFragmentActivity.EditNameDialogListener;
import com.st.nyamnew.adapters.StepPagerAdapter;
import com.st.nyamnew.factories.ApiFactory;
import com.st.nyamnew.factories.DataBaseFactory;
import com.st.nyamnew.factories.HttpFactory;
import com.st.nyamnew.models.Ingredient;
import com.st.nyamnew.models.Recipe;
import com.st.nyamnew.models.RecipeGeneral;
import com.st.nyamnew.models.Step;
import com.st.nyamnew.util.Constants;
import com.st.nyamnew.util.SanInputStream;

public class RecipeActivity extends SherlockFragmentActivity implements EditNameDialogListener {

	private String picture;
	private String TAG = "RecipeActivity";
	
	private RecipeGeneral recipeGeneral;
	private Recipe recipe;
	private String[] pictures;
	private ArrayList<Step> steps;
	private DataBaseFactory db;
	private NyamApplication application;
	private LayoutInflater inflater;
	private List<View> pages;
	
	private Bitmap bitmap = null;
	private String action;
	private boolean isSearchFavorites;
	private boolean isStillRequesting = true;
	private ViewPager pager;
	private DialogFragmentActivity editNameDialog;
	
	private TextView viewTitleRecipe;
	private TextView viewDescriptionRecipe;
	private TextView userViewRecipe;
	private ImageView viewPictureRecipe;
	private View recipeView;
	private TextView viewRatingRecipe;
	private TextView viewCooked_dishesRecipe;
	private TextView about; 
	
	private TextView viewStepBody;
	private TextView stepNumber;
	private ImageView viewPictureStep;
	private TextView recipeTitleStep;
	
	private int currentStep = 0;
	private int stepQuantity;
	private TextView lastDesc;
	private ProgressDialog progDailog;
	private Bitmap bitmapToPass;

	
	private ArrayList<Ingredient> ingredients;
	private boolean isExpanded = false;
	private TextView ingredientsNameV;
	private TextView ingredientsDataV;
	private LinearLayout ingredientsLinear;
	private RelativeLayout ingredientsRelative;
	private RelativeLayout mainRelative;
	private ImageView  orangeCircle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		action = getIntent().getAction();
		pager = new ViewPager(this);
		inflater = LayoutInflater.from(this);
		pages = new ArrayList<View>();
	    StepPagerAdapter adapter = new StepPagerAdapter(pages);
	    pager.setAdapter(adapter);
		pager.setCurrentItem(0);     
		setContentView(pager);
	
		
		application = (NyamApplication) getApplication();
		db = application.getDB();
		
		recipeView = inflater.inflate(R.layout.recipe_page, null);
		initRecipeViews(recipeView);
		Log.d(TAG, "AFTER INIT VIEWS");
		pages.add(recipeView);
		
		try {
			recipeGeneral = (RecipeGeneral) getIntent().getSerializableExtra("Recipe");
			Log.d(TAG, "recipeGeneral = " + recipeGeneral.toString());
			picture = Constants.URL + recipeGeneral.getImg_url();
			Log.d(TAG, "picture : " + picture);
			//Implement creating new button (delete) if this is View.gone
			
			if (action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
				Log.d(TAG, "Recipe favorites");
				//delete_button.setVisibility(View.VISIBLE);
				ingredients = (ArrayList<Ingredient>) db.getIngredientsByRecipeId(recipeGeneral.getId());
				fillIngredietns(ingredients);
				Bitmap bitmapViewPicture = BitmapFactory.decodeFile(Environment
						.getExternalStorageDirectory().toString()
						+ "/Nyam/NyamRecipesFavorites/"
						+ recipeGeneral.getImg_url().replace('/', '&'));
				bitmapToPass = bitmapViewPicture;
				Log.d(TAG, "recipeGeneral.getImg_url().replace('/', '&'): " + recipeGeneral.getImg_url().replace('/', '&'));
				Log.d(TAG, "Bitmap = " + bitmapViewPicture);
				viewPictureRecipe.setImageBitmap(bitmapViewPicture);
				viewDescriptionRecipe.setText(((Recipe) recipeGeneral).getDescription());
				viewTitleRecipe.setText(recipeGeneral.getTitle());
				Log.d(TAG, "Rating = " + recipeGeneral.getRating());
				viewRatingRecipe.setText(String.valueOf(recipeGeneral.getRating()));
				Log.d(TAG, "cooked dishes = " + recipeGeneral.getCooked_dishes_count());
				viewCooked_dishesRecipe.setText(String.valueOf(recipeGeneral.getCooked_dishes_count()));
				Log.d(TAG, "Title = " + recipeGeneral.getTitle());
				userViewRecipe.setText(((Recipe) recipeGeneral).getUser());
				Log.d(TAG, "User = " + ((Recipe) recipeGeneral).getUser());
				steps = db.getStepsByRecipeId(recipeGeneral.getId());
				
				
				
				if (steps != null) {
					((Recipe) recipeGeneral).setSteps(steps);
				}
				lastDesc = viewDescriptionRecipe;
				if (((Recipe) recipeGeneral).getDescription().equals("")) {
					recipeView.findViewById(R.id.about).setVisibility(View.GONE);
				}
				stepQuantity = steps.size();
				
				for (int i = 0; i < stepQuantity; i ++ ) {
					View stepView = inflater.inflate(R.layout.step_page, null);
					initStepViews(stepView);
					
					Step step = steps.get(i);
					if (step.getImg_url().equals(Constants.defaultPhoto) ||  step.getImg_url().equals(Constants.defaultPhoto1)) {
						String s = "<b>Шаг " + step.getNumber() + "</b>";
						lastDesc.append("\n");
						lastDesc.append("\n");
						lastDesc.append(Html.fromHtml(s));
						lastDesc.append("\n" + step.getInstruction());
					} else {
						currentStep++;
						Log.d(TAG, "isFavorites = 1");
						Bitmap bitmapViewPictureStep = BitmapFactory.decodeFile(
			        			Environment.getExternalStorageDirectory().toString() + 
			        			"/Nyam/NyamRecipesFavorites/" + step.getImg_url().replace('/', '&'));
						Log.d(TAG, "isFavorites = 2");
						Log.d(TAG, "Bitmap is favorite url = " + step.getImg_url());
						Log.d(TAG, "Bitmap is favorite = " + bitmapViewPictureStep);
						Log.d(TAG, "isFavorites = 3");
						viewPictureStep.setImageBitmap(bitmapViewPictureStep);
						Log.d(TAG, "Bitmap is favorite1 = " + bitmapViewPictureStep);
						
						Log.d(TAG, "isFavorites = 4");
						recipeTitleStep.setText(recipeGeneral.getTitle());
						viewStepBody.setText(step.getInstruction());
						stepNumber.setText(currentStep + "/" + stepQuantity);
						Log.d(TAG, "isFavorites = 5");
						lastDesc = viewStepBody;
						pages.add(stepView);
					}
				}
				setSupportProgressBarIndeterminateVisibility(false);
				Log.d(TAG, "setSupportProgressBarIndeterminateVisibility(false);");
				showButtonFav();
				//prepareIngredients();
			} else {
				Log.d(TAG,"Recipe not favorites");
				Log.d(TAG, "recipeGeneral else  = " + recipeGeneral.toString());
				String [] pictures = new String [] {"", ""};
				viewTitleRecipe.setText(recipeGeneral.getTitle());
				viewRatingRecipe.setText(String.valueOf(recipeGeneral.getRating()));
				viewCooked_dishesRecipe.setText(String.valueOf(recipeGeneral.getCooked_dishes_count()));
				new DownloadImageTask().execute(pictures);
				Log.d(TAG,"isFavorites = "+ getIntent().getBooleanExtra("isFavorites",false));
				Object[] params = new Object[] {this,Constants.URL + recipeGeneral.getPath()
								+ Constants.JSON };
				new AsyncHttpGetRecipe().execute(params);
				
				Log.d(TAG, "Rating = " + recipeGeneral.getRating());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	private void initStepViews(View stepView) {
		viewStepBody = (TextView)stepView.findViewById(R.id.step_body);
		stepNumber = (TextView)stepView.findViewById(R.id.step_number);
		viewPictureStep = (ImageView)stepView.findViewById(R.id.picture);
		recipeTitleStep = (TextView)stepView.findViewById(R.id.recipe_title);
	}
	
	private void initRecipeViews(View recipeView) {
		viewPictureRecipe = (ImageView) recipeView.findViewById(R.id.picture);
		viewDescriptionRecipe = (TextView) recipeView.findViewById(R.id.recepy_description);
		viewTitleRecipe = (TextView) recipeView.findViewById(R.id.recepy_title);
		viewRatingRecipe = (TextView) recipeView.findViewById(R.id.txtTitle2);
		viewCooked_dishesRecipe = (TextView) recipeView.findViewById(R.id.txtTitle1);
		userViewRecipe = (TextView) recipeView.findViewById(R.id.author_name);
		ingredientsLinear = (LinearLayout)recipeView.findViewById(R.id.ingredients_linear);
		ingredientsRelative = (RelativeLayout)recipeView.findViewById(R.id.ingredients_relative);
		about =  (TextView)recipeView.findViewById(R.id.about);
		orangeCircle = (ImageView)recipeView.findViewById(R.id.circle);
		orangeCircle.setBackgroundResource(R.drawable.expander_ic_minimized1);
		mainRelative = (RelativeLayout)recipeView.findViewById(R.id.main_relative);
		
		RelativeLayout relativeclic1 = (RelativeLayout)recipeView.findViewById(R.id.layoutClick);
        relativeclic1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(isExpanded) {
                	Log.d(TAG, "isExpanded");
                	ingredientsRelative.setVisibility(View.GONE);
                	isExpanded = false;
                	orangeCircle.setBackgroundResource(R.drawable.expander_ic_minimized1);
                }
                else {
                	Log.d(TAG, "not isExpanded");
                	ingredientsRelative.setVisibility(View.VISIBLE);
                	isExpanded = true;
                	orangeCircle.setBackgroundResource(R.drawable.expander_ic_maximized1);
                }
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.step_menu,  menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	private void showButton() {
		Log.d(TAG, "showButton");
		View bar = findViewById(R.id.progressbar_recipe);
		bar.setVisibility(View.GONE);
		if (db.isRecipeExists(recipeGeneral.getId())) {
			Log.d(TAG, "Racipe exists in favorites :");
        	Button buttonRemove = (Button)recipeView.findViewById(R.id.remove_button);
        	buttonRemove.setVisibility(View.VISIBLE);
        } else {
        	Button button = (Button)recipeView.findViewById(R.id.add_button);
        	button.setVisibility(View.VISIBLE);
        }
	}
	
	private void showButtonFav() {
		Log.d(TAG, "showButton");
		View bar = recipeView.findViewById(R.id.progressbar_recipe);
		bar.setVisibility(View.GONE);
		if (db.isRecipeExists(recipeGeneral.getId())) {
			Log.d(TAG, "Racipe exists in favorites :");
        	Button buttonRemove = (Button)recipeView.findViewById(R.id.remove_button);
        	buttonRemove.setVisibility(View.VISIBLE);
        } else {
        	Button button = (Button)recipeView.findViewById(R.id.add_button);
        	button.setVisibility(View.VISIBLE);
        }
	}
	
	/*
	private void prepareIngredients() {
		  SimpleExpandableListAdapter adapter;
		  final ExpandableListView list = (ExpandableListView)recipeView.findViewById(R.id.elvMain);
		  
		  ArrayList<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		  Map m  = new HashMap<String, String>();
		  m.put("groupName", "Ингридиенты");
		  groupData.add(m);
		  
		  
	      String groupFrom[] = new String[] {"groupName"};
	      
	      int groupTo[] = new int[] {R.id.groupname};
	     
	      
		 ArrayList<ArrayList<Map<String, String>>> childData = new ArrayList<ArrayList<Map<String, String>>>();
		 ArrayList<Map<String, String>> childDataItem = new ArrayList<Map<String, String>>();
	     for (Ingredient i : ingredients) {
	    	    Log.d(TAG, "Ingridient: " + i);
			    Map<String, String> m1 = new HashMap<String, String>();
			    StringBuilder ingredientsString = new StringBuilder();
			    String v = i.getValue();
				if (!v.equals("null")) {
					ingredientsString.append("- " + i.getName() + " " + v + i.getType());
				} else {
					ingredientsString.append("- " + i.getName() + " " + i.getType());
				}
			    m1.put("levelTwoCat", ingredientsString.toString());
			    childDataItem.add(m1);
	     }
	     childData.add(childDataItem);
	     
	     String childFrom[] = new String[] {"levelTwoCat"};
	     int childTo[] = new int[] {R.id.ingredients_second_layer_text};
	     
	     adapter = new SimpleExpandableListAdapter (
		            this,
		            groupData,
		            R.layout.ingredients_first_layer,
		            groupFrom,
		            groupTo,
		            childData,
		            R.layout.ingredients_second_layer,
		            childFrom,
		            childTo);
		
		 list.setAdapter(adapter);       
	}
	*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int current = pager.getCurrentItem();
		switch(item.getItemId())  {
		case R.id.next: 
			Log.d(TAG, "showNext()");
			pager.setCurrentItem(++current, true);
			return true;
		case R.id.previous: 
			Log.d(TAG, "showPrevious()");
			pager.setCurrentItem(--current, true);
			return true;
		case R.id.start_step: 
			Log.d(TAG, "start");
			pager.setCurrentItem(0, true);
			return true;
		case R.id.end_step: 
			Log.d(TAG, "end");
			pager.setCurrentItem(stepQuantity, true);
			return true;
		case 16908332: 
			Log.d(TAG, "logo");
			startActivity(new Intent(this, DashboardActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.add_button:
			Log.d(TAG, "Pressed save to favorites");
			if (!action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
					Log.d(TAG, "Add button pressed Network eavailable");
					if (NyamApplication.isPasswordExists()) {
						Log.d(TAG, "application.isPasswordExists(): " + application.isPasswordExists());
						Log.d(TAG, "Pressed save to favorites password exists");
						if (recipe != null && steps != null) {
							recipe.setSteps(steps);
							db.addRecipeToFavorites(recipe, bitmap);
						}
						Object[] params = new Object[] {this, recipe.getId() };
						new AddToFavorites().execute(params);
						Log.d(TAG, "Add t fav pressed");
						changeIntoRemveButton();
			        	Intent intent = getIntent();
						intent.putExtra("deletedRecipe", recipeGeneral);
						setResult(Constants.RESULT_OK_ADD_FAV, intent);
					}
					else {
						Log.d(TAG, "Pressed save to favorites password not exists");
						showDialog();
					}
				} else {
					//Show dialog: "Network unavailable"
				}
			break;
		case R.id.remove_button:
			if (HttpFactory.isNetworkAvailable(this)) {
				if (action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
					Log.d(TAG, "Delete button pressed");
					int item_id = db.getItemIdById(recipeGeneral.getId());
					db.deleteRecipeFromFavorites((Recipe)recipeGeneral);
					Object[] params = new Object[] {this, item_id};
					Log.d(TAG, "recipeGeneral.getPath(): " + recipeGeneral.getPath());
					Log.d(TAG, "recipeGeneral.item_id: " + item_id);
					new DeleteFromFavorites().execute(params);
					Intent intent = getIntent();
					intent.putExtra("deletedRecipe", recipeGeneral);
					Log.d(TAG, "Extra putted");
					setResult(RESULT_OK, intent);
					Log.d(TAG, "Result_OK sent");
					finish();
				} else {
					Log.d(TAG, "Delete button pressed");
					int item_id = db.getItemIdById(recipeGeneral.getId());
					db.deleteRecipeFromFavorites((Recipe)recipe);
					changeIntoAddButton();
					Object[] params = new Object[] {this, item_id};
					Log.d(TAG, "recipeGeneral.getPath(): " + recipeGeneral.getPath());
					Log.d(TAG, "recipeGeneral.item_id: " + item_id);
					new DeleteFromFavorites().execute(params);
					Intent intent = getIntent();
					intent.putExtra("deletedRecipe", recipeGeneral);
					Log.d(TAG, "Extra putted");
					setResult(RESULT_OK, intent);
					Log.d(TAG, "Result_OK sent");
				}
			}
		   
			else {
				Log.d(TAG, "Delete button pressed");
					Log.d(TAG, "Delete button pressed Network eavailable");
					if (NyamApplication.isPasswordExists()) {
						if (action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
							Log.d(TAG, "Delete button pressed Password exist");
							db.deleteRecipeFromFavorites((Recipe)recipeGeneral);
							Intent intent = getIntent();
							intent.putExtra("deletedRecipe", recipeGeneral);
							setResult(Constants.RESULT_OK_REMOVE_FAV, intent);
							Log.d(TAG, "Extra putted");
							finish();
						}
					}
					else {
						showDialog();
					}
			}
		break;
		/*
		case R.id.show_ingredients:
			Log.d(TAG, "Pressed show Ingredients");
			Intent intentIngredients = new Intent(this,
					IngredientsActivity.class);
			intentIngredients.putExtra("ingredients", ingredients);
			intentIngredients.putExtra("bitmap", bitmapToPass);
			startActivity(intentIngredients);
			break;
			*/
		}
		
	}
	
	
	
	private void changeIntoAddButton() {
		Button button = (Button)recipeView.findViewById(R.id.remove_button);
    	button.setVisibility(View.GONE);
    	Button buttonRemove = (Button)recipeView.findViewById(R.id.add_button);
    	buttonRemove.setVisibility(View.VISIBLE);
	}
	
	private void changeIntoRemveButton() {
		Button button = (Button)recipeView.findViewById(R.id.add_button);
    	button.setVisibility(View.GONE);
    	Button buttonRemove = (Button)recipeView.findViewById(R.id.remove_button);
    	buttonRemove.setVisibility(View.VISIBLE);
	}
	
	public void showDialog() {
		android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
		editNameDialog = new DialogFragmentActivity();
        editNameDialog.show(fm, "fragment_edit_name");
	}
	
	@Override
    public void onFinishEditDialog(String login, String password) {
        try {
        	String [] params = {login, password};
        	new LoginingCheckout().execute(params);
        } catch (Exception e) {}
    }
	
	public void clearFields() {
		if (editNameDialog != null) {
			editNameDialog.clearFialds();
		}
	}
	/*
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_further:
			if (recipe == null || steps == null) {
				Log.d(TAG, "recipe: " + recipe);
				Log.d(TAG, "steps: " + steps);
			}
			Log.d(TAG, "In menu_further");
			Intent stepIntent = new Intent(this, StepActivity.class);

			if (action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
				Log.d(TAG, "id: " + recipeGeneral.getId());
				Log.d(TAG, "desc: " + recipeGeneral.getTitle());
				stepIntent.putExtra("Recipe1", recipeGeneral);
				stepIntent.putExtra("isFavorites", true);
			} else {
				if (recipe != null) {
					Log.d(TAG, "id: " + recipe.getId());
					Log.d(TAG, "desc: " + recipe.getTitle());
					recipe.setSteps(steps);
					stepIntent.putExtra("Recipe1", recipe);
		
				}
			
			}
			startActivity(stepIntent);
			return true;
		case R.id.catalog:
			if(HttpFactory.isNetworkAvailable(this)) {
				Intent mainCategoryIntent = new Intent(this, ExpandableCategoriesActivity.class);
				startActivity(mainCategoryIntent);
			} else {
				showDialog(Constants.DAILOG_INTERNET_UNAVAILABLE);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	*/
	
	private void fillIngredietns(List<Ingredient> ingredients) {
		LayoutInflater inflater1 = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (int i = 0; i < ingredients.size(); i++ ) {
				Ingredient in = ingredients.get(i); 
				FrameLayout ingredients_frame = (FrameLayout)inflater1.inflate(R.layout.ingredient_frame_layout, null);
			    ingredientsNameV = (TextView)ingredients_frame.findViewById(R.id.ingredient_name);
			    ingredientsDataV = (TextView)ingredients_frame.findViewById(R.id.ingredient_data);
			    String v = in.getValue();
			    ingredientsNameV.setText(in.getName());
				if (!v.equals("null")) {
					ingredientsDataV.setText(v + " " +  in.getType());
				} else {
					ingredientsDataV.setText(in.getType());
				}
				ingredientsLinear.addView(ingredients_frame);
		 }
		
	}
	
	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... str) {
			try {
				InputStream in = new java.net.URL(picture).openStream();
				bitmap = BitmapFactory.decodeStream(new SanInputStream(in));
				// viewPicture.setImageBitmap(bitmap);
				bitmapToPass = bitmap;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			viewPictureRecipe.setBackgroundDrawable(new BitmapDrawable(bitmap));
		}
	}
	

	private class AsyncHttpGetRecipe extends AsyncTask<Object, String, Recipe> {

		@Override
		protected Recipe doInBackground(Object... params) {
			Recipe recipeAsync = null;
			try {
				recipeAsync = ApiFactory.getRecipe((Context) params[0],
						(String) params[1]);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			recipe = recipeAsync;
			return recipeAsync;
		}

		@Override
		protected void onPostExecute(Recipe recipeTemp) {
			ingredients = recipeTemp.getIngredients();
			fillIngredietns(ingredients);
			
			String description = recipeTemp.getDescription();
			if(description.equals("")) {
				recipeView.findViewById(R.id.about).setVisibility(View.GONE);
			}
			viewDescriptionRecipe.setText(description);
			Log.d(TAG, "Description = " + recipeTemp.getDescription());
			userViewRecipe.setText(recipeTemp.getUser());
			lastDesc = viewDescriptionRecipe;
			Log.d(TAG, "User = " + recipeTemp.getUser());
			Object[] params = new Object[] {RecipeActivity.this,Constants.URL + recipeGeneral.getPath()
					+ Constants.JSON };
			new AsyncHttpGetSteps().execute(params);

		}
	}

	private class AsyncHttpGetSteps extends AsyncTask<Object, String, ArrayList<Step>> {

		@Override
		protected ArrayList<Step> doInBackground(Object... params) {
			ArrayList<Step> stepsAsync = null;
			try {
				stepsAsync = ApiFactory.getSteps((Context) params[0],
						(String) params[1]);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return stepsAsync;
		}

		@Override
		protected void onPostExecute(ArrayList<Step> stepsAsync) {
			steps = stepsAsync;
			stepQuantity = steps.size();
			Log.d(TAG,"stepQuantity: " + steps.size());
			
			for (int i= 0; i < stepQuantity; i ++ ) {
				View stepView = inflater.inflate(R.layout.step_page, null);
				initStepViews(stepView);
				Step step = steps.get(i);
				
				if (step.getImg_url().equals(Constants.defaultPhoto) ||  step.getImg_url().equals(Constants.defaultPhoto1)) {
					String s = "<b>Шаг " + step.getNumber() + "</b>";
					lastDesc.append("\n");
					lastDesc.append("\n");
					lastDesc.append(Html.fromHtml(s));
					lastDesc.append("\n" + step.getInstruction());
					currentStep++;
					
				} else {
					currentStep++;
					Log.d(TAG, "isFavorites = 1");
					Log.d(TAG, "Constants.URL + step.getImg_url() = " + Constants.URL + step.getImg_url());
					Object [] pictures = new Object [] {(String)Constants.URL + step.getImg_url(), (View)viewPictureStep};
					new DownloadImageStepTask().execute(pictures);
					
					recipeTitleStep.setText(recipeGeneral.getTitle());
					viewStepBody.setText(step.getInstruction());
					stepNumber.setText(currentStep + "/" + stepQuantity);
					
					lastDesc = viewStepBody;
					pages.add(stepView);
				}
				
				
				/*
				if (action.equals(Constants.ACTION_FAVORITE_RECIPES)) {
					Log.d(TAG, "isFavorites = " + getIntent().getBooleanExtra("isFavorites", false));
					Bitmap bitmapViewPicture = BitmapFactory.decodeFile(
		        			Environment.getExternalStorageDirectory().toString() + 
		        			"/Nyam/NyamRecipesFavorites/" + step.getImg_url().replace('/', '&'));
					Log.d(TAG, "Bitmap is favorite url = " + step.getImg_url());
					Log.d(TAG, "Bitmap is favorite = " + bitmapViewPicture);
					viewPictureStep.setImageBitmap(bitmapViewPicture);
					Log.d(TAG, "Bitmap is favorite1 = " + bitmapViewPicture);
				} else {
				*/
			
				//}
				/*
				currentStep++;
				recipeTitleStep.setText(recipeGeneral.getTitle());
				viewStepBody.setText(step.getInstruction());
				stepNumber.setText(currentStep + "/" + stepQuantity);
				pages.add(stepView);
				*/
				
			}
			isStillRequesting = false;
			setSupportProgressBarIndeterminateVisibility(false);
			showButton();
		}
	}
	
private class DownloadImageStepTask extends AsyncTask<Object,Void,Object[]> {
		
		Object [] outObjects;
		
		@Override
		protected Object[] doInBackground(Object...  o) {
			Bitmap outBitmap = null;
			try {   
				InputStream in = new java.net.URL((String)o[0]).openStream();
				outBitmap = BitmapFactory.decodeStream(new SanInputStream(in));
				outObjects = new Object [] {outBitmap, o[1]};
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return outObjects;
		}
		
	
		
		@Override 
		protected void onPostExecute(Object[] result) {
			super.onPostExecute(result);
			((View)result[1]).setBackgroundDrawable(new BitmapDrawable((Bitmap)result[0]));
		}
	}

	private class AddToFavorites extends AsyncTask<Object, String, Boolean> {
	
		@Override
		protected Boolean doInBackground(Object... params) {
			Boolean isAdded = false;
			try {
				
				int item_id = HttpFactory.sendAddToFavorites((Context) params[0],(Integer)params[1]);
				Log.d(TAG, "AddToFavorites.item_id = " + item_id);
				db.insertItemId((Integer)params[1], item_id);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return isAdded;
		}
		
	}

	private class DeleteFromFavorites extends AsyncTask<Object, String, Boolean> {
	
		@Override
		protected Boolean doInBackground(Object... params) {
			Boolean isAdded = null;
			try {
				HttpFactory.sendDeleteFromFavorites((Context) params[0], (Integer)params[1]);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return isAdded;
		}
	}

	private class LoginingCheckout extends AsyncTask<String, Void, Boolean> {
	
		@Override
		protected Boolean doInBackground(String... params) {
			boolean log = false;
			try {
				log = HttpFactory.isLoginned(params[0], params[1]);
				if (log) {
					application.setLogin(params[0]);
			        application.setPassword(params[1]);
			        application.storeLoginPassword();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return log;
		}
		
		@Override 
		protected void onPostExecute(Boolean result) {
			if (result) {
				editNameDialog.dismiss();
			} else {
				RecipeActivity.this.clearFields();
			}
		}
	}
	

}
