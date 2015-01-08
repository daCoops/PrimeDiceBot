package PrimeBot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.text.GapContent;

import org.json.JSONObject;

public class PrimeBot {

	private final String USER_AGENT = "Mozilla/5.0";
	private static String accessToken;
	public static String username;
	public static String password;
	
	public static final String LESS = "%3C";
	public static final String BIG = "%3E";
	
	public static String direction = LESS;	

	private static UserStats meStats = new UserStats();

	public static int delay = 290;
	public static double nextBetAmount;
	public static double target = 20;
	public static int baseBet = 40;
	public static double increaseOnLossMultiplier = 1.25;

	public static int successHTTP = 0;

	public static double sessionProfitTarget = 100000;

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static void main(String[] args) throws Exception 
	{
		PrimeBot primeBot = new PrimeBot();

		if(args.length == 0)
		{
			System.out.println("No parameters supplied.");
			System.out.println("java -jar PrimeBot.jar [username] [password] [base bet] [target] [increase on loss multiplier] [profit target]");
			System.out.println("e.g.");
			System.out.println("java -jar PrimeBot.jar Bob Bob123 1 51.5 2.1 10000");
			System.out.println("\t[Base Bet]:\n\t\tYour Base Bet as an integer. entering a value of 1, represents PrimeDice lowest bet - 0.00000001 BTC");
			System.out.println("\t[Target]:\n\t\tThe Win Chance. e.g. 51.5 represents a win chance of 51.5%");
			System.out.println("\t[Increase On Loss Multiplier]:\n\t\tThe Factor increase to increase your bet on loss. CAUTION! This is a multiplier and NOT a Percentage. e.g. 2.1 Will Multiply Previous Bet * 2.1 on loss for next wager");
			System.out.println("\t[Profit Target]:\n\t\tTarget Profit for the session.");
			System.out.println();
			System.out.println("Reminder of how this works..");
			System.out.println("You set the Base Bet Amount, Profit Target, and Win Chance, and then the bot will bet as fast as it can until profit target has been reached.");
			System.out.println("On a loss, it will increase the base bet by a factor of the Multiplier entered. On win, it resets back to base.");
			System.exit(0);
		}
		else
		{
			try {
				username = args[0];
				password = args[1];
				baseBet = Integer.parseInt(args[2]);
				target = Double.parseDouble(args[3]);
				increaseOnLossMultiplier = Double.parseDouble(args[4]);
				sessionProfitTarget = Double.parseDouble(args[5]);
			}
			catch(Exception e)
			{
				System.out.println("Problem reading arguments. Please try again.");
				System.exit(0);
			}
		}

		java.security.Security.setProperty("networkaddress.cache.ttl" , "0");

		primeBot.doLogin();

		try {
			Thread.sleep(2000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		primeBot.doStats();
		try {
			Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		nextBetAmount = baseBet;
		JSONObject currentBet = new JSONObject();
		
		int lostCount = 0;

		while (meStats.getProfitSession() < sessionProfitTarget)
		{
			// Main betting loop
			try
			{
				currentBet = primeBot.doBet();
			}
			catch(Exception e )
			{
				System.out.println(e.getMessage());
				try {
					Thread.sleep(2000);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			if (currentBet != null)
			{
				successHTTP = successHTTP + 1;
				meStats.updateProfitSession(currentBet.getDouble("profit"));
				if (!currentBet.getBoolean("win"))
				{
					//System.out.println("Lost Bet");
					nextBetAmount = Math.round(nextBetAmount * increaseOnLossMultiplier);
					//System.out.println("Next Bet Is = " + nextBetAmount);
					lostCount = lostCount + 1;
					if (lostCount == 5)
					{
						if (direction.equals(LESS))
						{
							direction = BIG;
						}
						else
						{
							direction = LESS;
						}
					}
				}
				else
				{
					nextBetAmount = baseBet;
					lostCount = 0;
				}

				System.out.print(sdf.format(new Date()) + "  ");
				System.out.format("LastBetProfit: %22f  SessionProfit: %22f  Delay: %d  Direction: %s", currentBet.getDouble("profit"), meStats.getProfitSession(), delay,direction);
				System.out.println();
				//System.out.println(sdf.format(new Date()) + "t LastBetProfit = \t" + currentBet.getDouble("profit") + "\t  :\t   Session Profit = \t\t" + meStats.getProfitSession() + "\t\t Delay = \t" + delay);

				if (successHTTP == 20)
				{
					delay = delay -2;
					successHTTP = 0;
				}

				try {
					Thread.sleep(delay);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}

			}
			else
			{
				successHTTP = 0;
				//System.out.println("Too fast....");
				try {
					Thread.sleep(10);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				delay = delay + 2;
			}
		}


	}

	// Login
	private void doLogin() throws Exception 
	{

		String url = "https://api.primedice.com/api/login";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "username=" + username + "&password=" + password;

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();


		//print result
		System.out.println(response.toString());

		JSONObject loginJsonResp = new JSONObject(response.toString());
		accessToken = loginJsonResp.getString("access_token");
		System.out.println(accessToken);

	}

	private JSONObject doBet() throws Exception 
	{
		String url = "https://api.primedice.com/api/bet?access_token=" + accessToken;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("origin","https://primedice.com");
		con.setRequestProperty("referer","https://primedice.com/play");
		con.setRequestProperty("cache-control","no-cache"); 
		con.setRequestProperty("pragma","no-cache");

		String urlParameters = "amount="+nextBetAmount + "&condition=" + direction + "&target=" + target;

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//System.out.println("Betting HTTP Response Code : " + responseCode);

		if (responseCode == 200)
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONObject betResult = new JSONObject(response.toString());
			return betResult.getJSONObject("bet");
		}
		else
		{
			return null;
		}

	}

	private void doStats() throws Exception 
	{
		String url = "https://api.primedice.com/api/users/1?access_token=" + accessToken;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("origin","https://primedice.com");
		con.setRequestProperty("referer","https://primedice.com/play");
		con.setRequestProperty("cache-control","no-cache"); 
		con.setRequestProperty("pragma","no-cache");

		int responseCode = con.getResponseCode();
		System.out.println("Betting HTTP Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		System.out.println(response.toString());

		// Parse them to the UserStas Obj - only want the "users" JSON obj
		JSONObject userStats = new JSONObject(response.toString()).getJSONObject("user");
		meStats.parseStats(userStats);

	}

	public static class UserStats {

		private double balance;
		private double wagered;
		private double profit;
		private int bets;
		private int wins;
		private int losses;
		private double wageredSession;
		private double profitSession;
		private int betsSession;
		private int winsSession;
		private int lossesSession;

		public UserStats() 
		{
			super();
			this.balance = 0;
			this.wagered = 0;
			this.profit = 0;
			this.bets = 0;
			this.wins = 0;
			this.losses = 0;
			this.wageredSession = 0;
			this.profitSession = 0;
			this.betsSession = 0;
			this.winsSession = 0;
			this.lossesSession = 0;
		}

		public void parseStats(JSONObject userStats)
		{
			this.balance = userStats.getDouble("balance");
			this.wagered = userStats.getDouble("wagered");
			this.profit =userStats.getDouble("profit");
			this.bets = userStats.getInt("bets");
			this.wins = userStats.getInt("wins");
			this.losses = userStats.getInt("losses");
		}

		public void resetSessionStats()
		{
			this.wageredSession = 0;
			this.profitSession = 0;
			this.betsSession = 0;
			this.winsSession = 0;
			this.lossesSession = 0;
		}

		public double getBalance() {
			return balance;
		}
		public void setBalance(double balance) {
			this.balance = balance;
		}
		public double getWagered() {
			return wagered;
		}
		public void setWagered(double wagered) {
			this.wagered = wagered;
		}
		public double getProfit() {
			return profit;
		}
		public void setProfit(double profit) {
			this.profit = profit;
		}
		public int getBets() {
			return bets;
		}
		public void setBets(int bets) {
			this.bets = bets;
		}
		public int getWins() {
			return wins;
		}
		public void setWins(int wins) {
			this.wins = wins;
		}
		public int getLosses() {
			return losses;
		}
		public void setLosses(int losses) {
			this.losses = losses;
		}
		public double getWageredSession() {
			return wageredSession;
		}
		public void setWageredSession(double wageredSession) {
			this.wageredSession = wageredSession;
		}
		public double getProfitSession() {
			return profitSession;
		}
		public void setProfitSession(double profitSession) {
			this.profitSession = profitSession;
		}
		public int getBetsSession() {
			return betsSession;
		}
		public void setBetsSession(int betsSession) {
			this.betsSession = betsSession;
		}
		public int getWinsSession() {
			return winsSession;
		}
		public void setWinsSession(int winsSession) {
			this.winsSession = winsSession;
		}
		public int getLossesSession() {
			return lossesSession;
		}
		public void setLossesSession(int lossesSession) {
			this.lossesSession = lossesSession;
		}

		@Override
		public String toString() {
			return "UserStats [balance=" + balance + ", wagered=" + wagered
					+ ", profit=" + profit + ", bets=" + bets + ", wins=" + wins
					+ ", losses=" + losses + ", wageredSession=" + wageredSession
					+ ", profitSession=" + profitSession + ", betsSession="
					+ betsSession + ", winsSession=" + winsSession
					+ ", lossesSession=" + lossesSession + "]";
		}

		public void updateProfitSession(double betProfit) 
		{
			this.profitSession = this.profitSession + betProfit;
		}


	}

}
