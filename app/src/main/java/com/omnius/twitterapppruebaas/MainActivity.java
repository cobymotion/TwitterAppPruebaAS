package com.omnius.twitterapppruebaas;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.omnius.twitterapppruebaas.R;

public class MainActivity extends Activity {

	private Button						botonConectar;
	private TextView					textoTweet;

	private static Twitter				twitter;
	private static RequestToken			peticionToken;
	private static SharedPreferences	preferenciasCompartidas;

	private EditText					texto;

	private Button						boton;

	int TWITTER_AUTH = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// se permiten las operaciones de red en el hilo principal
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(policy);

		//Se obtiene el objeto que manejara las sharedPreferences
		preferenciasCompartidas = getSharedPreferences(Constantes.NOMBRE_ARCHIVO_PREFERENCIAS, MODE_PRIVATE);
		
		textoTweet = (TextView) findViewById(R.id.textoTweet);
		
		botonConectar = (Button) findViewById(R.id.conectarTwitter);
		botonConectar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (estaConectado()) {
					desconectarTwitter();
					botonConectar.setText(R.string.conectar);
				} else {
					obtieneAutorizacionOAuth();
				}
			}
		});

		texto = (EditText) findViewById(R.id.textoTweet);
		
		boton = (Button) findViewById(R.id.enviarTweet);
		boton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				enviaTweet();
			}
		});

		/**
		 * Cuando se inicia esta App despues de la autorizacion de twitter
		 * se obtiene el resultado de la Activity de autorizacion
		 */ 
		Uri uri = getIntent().getData();
		if (uri != null && uri.toString().startsWith(Constantes.CALLBACK_URL)) {
			String verifier = uri.getQueryParameter(Constantes.VERIFICADOR_OAUTH_DATOS_EXTRA);
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(peticionToken, verifier);
				Editor e = preferenciasCompartidas.edit();
				
				// se obtienen y guardan los 2 tokens que serviran para firmar peticiones con la cuenta autorizada
				e.putString(Constantes.PREFERENCIA_KEY_TOKEN, accessToken.getToken());
				e.putString(Constantes.PREFERENCIA_KEY_SECRET, accessToken.getTokenSecret());
				e.commit();
			} catch (Exception e) {
				Log.e("TwitterAppPrueba", "Error: "+e.toString());
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (estaConectado()) {
			botonConectar.setText(R.string.desconectar);
		} else {
			botonConectar.setText(R.string.conectar);
		}
	}
	
	/**
	 * Comprueba que el Key_Token este guardado en las sharedPreferences
	 */
	private boolean estaConectado() {
		if (preferenciasCompartidas.getString(Constantes.PREFERENCIA_KEY_TOKEN, null) != null){
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Se eliminan la llave Token y la llave Secret de las SharedPreferences
	 */
	private void desconectarTwitter() {
		SharedPreferences.Editor editor = preferenciasCompartidas.edit();
		editor.remove(Constantes.PREFERENCIA_KEY_TOKEN);
		editor.remove(Constantes.PREFERENCIA_KEY_SECRET);
		editor.commit();
	}

	/**
	 * Se lanza el navegador para que el usuario autorize la aplicacion..
	 */
	private void obtieneAutorizacionOAuth() {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setOAuthConsumerKey(Constantes.CONSUMER_KEY);
		configurationBuilder.setOAuthConsumerSecret(Constantes.CONSUMER_SECRET);
		
		Configuration configuration = configurationBuilder.build();
		twitter = new TwitterFactory(configuration).getInstance();

		try {
			peticionToken = twitter.getOAuthRequestToken(Constantes.CALLBACK_URL);
			Toast.makeText(this, "Debes autorizar esta App", Toast.LENGTH_LONG).show();
			Intent i = new Intent(this,PaginaTwitter.class);
			i.putExtra("URL",peticionToken.getAuthenticationURL());
			startActivityForResult(i,TWITTER_AUTH);
		} catch (TwitterException e) {
			Log.e("twitter", "Exception = " + e);
		} catch (Exception e) {
			Log.e("twitter", "Exception = " + e);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == TWITTER_AUTH) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Aplicacion autorizada", Toast.LENGTH_LONG).show(); //Se obtienen los datos extras de PaginaTwitter.
				String oauthVerifier = (String) data.getExtras().get("oauth_verifier"); AccessToken at = null;
				try {
// se obtienen los 2 tokens que servirán para firmar peticiones con la cuenta autorizada
                    at = twitter.getOAuthAccessToken(oauthVerifier);
					String theToken = at.getToken();
					String theTokenSecret = at.getTokenSecret();

					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
					settings = PreferenceManager.getDefaultSharedPreferences(this);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("twitter_access_token",theToken);
					editor.putString("twitter_access_token_secret", theTokenSecret);
					editor.commit();
				}
				catch (Exception e) {
					Log.d("twapp", e.toString()); }
			} }
	}

	/**
	 * Se envia el tweet	
	 */
	public void enviaTweet() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String oauthAccessToken = settings.getString("twitter_access_token", null );
		String oAuthAccessTokenSecret = settings.getString( "twitter_access_token_secret", null );

		ConfigurationBuilder confbuilder = new ConfigurationBuilder();
		Configuration conf = confbuilder.setDebugEnabled(true)
								.setOAuthConsumerKey(Constantes.CONSUMER_KEY)
								.setOAuthConsumerSecret(Constantes.CONSUMER_SECRET)
								.setOAuthAccessToken(oauthAccessToken)
								.setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
								.build();

		//se obtiene un nuevo objeto Twitter a partir de la configuracion anterior
		Twitter twitter = new TwitterFactory(conf).getInstance();

		try {
			// se realiza el envio del tweet.
			twitter.updateStatus(texto.getText().toString());

		} catch (TwitterException e) {
			Log.e("twitter", e.toString());
		}
		Toast.makeText(this, "Tweet exitoso", Toast.LENGTH_LONG).show();
	}
}
