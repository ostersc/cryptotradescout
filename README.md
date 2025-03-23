# Crypto-Algo-Trading Platform

A comprehensive cryptocurrency algorithmic trading platform leveraging AI to empower traders with intelligent strategy development and advanced analytics.

![Platform Screenshot](screenshot.png)

## 🚀 Features

### Advanced Trading Capabilities
- **Multiple Exchange Integration**: Connect with major exchanges (Kraken, Coinbase)
- **Real-Time Market Data**: Live price tracking and chart visualization
- **Algorithmic Trading**: Deploy multiple trading strategies
- **Backtesting Engine**: Test strategies against historical data
- **Automated Trade Execution**: Set up and run automated trading sessions

### AI-Powered Trading Intelligence
- **Strategy Analysis**: AI-driven trading strategy recommendations
- **Market Sentiment Analysis**: Advanced insights on market conditions
- **Parameter Optimization**: Smart suggestion of strategy parameters
- **Risk Assessment**: Intelligent risk/reward analysis

### Comprehensive Analytics
- **Performance Metrics**: Detailed statistics on trading performance
- **Tax & Fee Tracking**: Automatic calculation of taxes and exchange fees
- **Visualization Tools**: Interactive charts and customizable dashboards
- **Trade History**: Complete record of trades and performance history

### Risk Management
- **Position Sizing**: Configurable position size controls
- **Stop-Loss Management**: Automated stop-loss implementation
- **FIFO Accounting**: Proper tracking of tax implications
- **Fee Optimization**: Smart routing to minimize trading costs

## 🛠️ Technology Stack

### Backend
- **Spring Boot**: Core application framework
- **Java 21**: Latest Java version with enhanced features
- **H2 Database**: In-memory database for development and testing
- **WebFlux**: Reactive programming for handling API requests
- **Spring Security**: Authentication and authorization

### Frontend
- **HTML5/CSS3/JavaScript**: Modern web standards for UI
- **Bootstrap 5**: Responsive design framework
- **Chart.js**: Interactive data visualization
- **Vanilla JS**: Efficient frontend functionality

### AI Integration
- **OpenAI API**: GPT-4o model integration for strategy recommendations
- **Technical Indicators**: Over 30 built-in technical indicators

### Exchange Connectivity
- **Kraken API**: Real-time market data and order execution
- **Coinbase API**: Additional exchange connectivity

### Development Tools
- **Maven**: Dependency management and build automation
- **JUnit 5**: Comprehensive test framework
- **Logback**: Flexible logging configuration
- **Git**: Version control and collaboration

## 📋 Available Trading Algorithms

1. **Simple Moving Average Crossover**
   - Uses the crossover of two moving averages to generate buy/sell signals

2. **Bollinger Bands**
   - Volatility-based strategy using standard deviation bands around a moving average

3. **Relative Strength Index (RSI)**
   - Momentum oscillator measuring speed and change of price movements

4. **Exchange Arbitrage**
   - Exploits price differences of the same asset across multiple exchanges

## 🔧 Installation & Setup

### Prerequisites
- JDK 21 or higher
- Maven 3.8+
- Git

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/yourusername/crypto-algo-trading.git
cd crypto-algo-trading

# Build the project
./mvnw clean package
```

### Run the Application
```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:5000/`

## 💻 Usage

### Dashboard
The main dashboard provides an overview of the current market status, active trading strategies, and performance metrics.

### Trading Setup
1. Navigate to the "Trading" tab
2. Select an exchange and trading pair
3. Choose a trading algorithm
4. Configure algorithm parameters
5. Start live trading

### Backtesting
1. Go to the "Backtest" tab
2. Select a date range and trading pair
3. Choose an algorithm and set parameters
4. Run the backtest to see historical performance

### AI Advisor
1. Navigate to the "AI Advisor" tab
2. The system will analyze current market conditions
3. Review AI-recommended trading strategies
4. Apply suggested parameters to trading or backtesting

## 🔒 Security & API Keys

The platform requires API keys from exchanges to access live data and execute trades. These keys are stored securely and never exposed to the client side.

To add your API keys:
1. Create a `application-secrets.properties` file in the `src/main/resources` directory
2. Add your API keys in the following format:
```
kraken.api.key=YOUR_KRAKEN_API_KEY
kraken.api.secret=YOUR_KRAKEN_API_SECRET
coinbase.api.key=YOUR_COINBASE_API_KEY
coinbase.api.secret=YOUR_COINBASE_API_SECRET
openai.api.key=YOUR_OPENAI_API_KEY
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Contact

For questions or support, please open an issue in the repository or contact the maintainers directly.

---

**Note**: This software is for educational and research purposes only. Trading cryptocurrencies involves significant risk, and past performance is not indicative of future results. Always do your own research before trading.