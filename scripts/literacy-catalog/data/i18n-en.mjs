/** English translations for literacy catalog terms. */
export const I18N_EN = {
  'getiri': {
    en: {
      title: 'Return',
      shortDescription: 'The rate or amount of gain an investment produces over a given period.',
      detailedDescription: 'Return can come from price appreciation, interest, dividends, or exchange-rate moves. It is the core metric for performance comparisons in the portal.',
      howToInterpret: 'Percentage return shows change relative to the starting value. Always read it together with the period length (daily, monthly, annual).',
      commonMistake: 'Treating a strong short-term return as evidence of sustainable long-term performance.',
      relatedTerms: ['Nominal return', 'Real return', 'Risk', 'Volatility'],
    },
  },
  'risk': {
    en: {
      title: 'Risk',
      shortDescription: 'The likelihood that expected returns will not materialize or will result in a loss.',
      detailedDescription: 'Risk is measured across dimensions such as volatility, liquidity, and credit risk. Portfolio allocation is the primary tool for managing it.',
      howToInterpret: 'Higher risk may offer higher potential return, but it also increases the chance of swings and losses.',
      commonMistake: 'Seeing risk only as “losing money” and overlooking liquidity and currency risk.',
      relatedTerms: ['Volatility', 'Liquidity', 'Portfolio'],
    },
  },
  'likidite': {
    en: {
      title: 'Liquidity',
      shortDescription: 'How quickly and cheaply an asset can be converted into cash.',
      detailedDescription: 'When liquidity is high, the bid-ask spread is usually tighter; when it is low, slippage and transaction costs tend to rise.',
      howToInterpret: 'Higher liquidity generally means a narrower spread; lower liquidity increases the risk of price slippage.',
      commonMistake: 'Placing large orders in thinly traded assets.',
      relatedTerms: ['Spread', 'Volatility'],
    },
  },
  'faiz': {
    en: {
      title: 'Interest',
      shortDescription: 'The rate paid or earned for the use of money over a set period.',
      detailedDescription: 'Interest rates determine borrowing costs and deposit or bond yields, and they are closely linked to macro policy and inflation.',
      howToInterpret: 'As interest rates rise, borrowing becomes more expensive and deposit and bond yields usually move higher.',
      relatedTerms: ['Policy rate', 'Deposit rate', 'Real interest rate'],
    },
  },
  'nominal-deger': {
    en: {
      title: 'Nominal value',
      shortDescription: 'An amount expressed in currency terms without adjusting for inflation.',
      detailedDescription: 'Nominal figures make comparison straightforward, but they do not reflect changes in purchasing power.',
      howToInterpret: 'A nominal increase does not necessarily mean greater purchasing power; read it alongside real value.',
      relatedTerms: ['Real value', 'CPI index', 'Inflation'],
    },
  },
  'reel-deger': {
    en: {
      title: 'Real value',
      shortDescription: 'An amount expressed in purchasing-power terms after removing inflation effects.',
      detailedDescription: 'Real value shows how much goods and services money can actually buy.',
      howToInterpret: 'If real value is falling, purchasing power may be weakening even when the nominal amount rises.',
      commonMistake: 'Treating nominal gains as true gains in wealth.',
      relatedTerms: ['Real return', 'CPI index', 'Purchasing power'],
    },
  },
  'alim-gucu': {
    en: {
      title: 'Purchasing power',
      shortDescription: 'The quantity of goods and services that a given income or savings can buy.',
      detailedDescription: 'Purchasing power shifts with inflation, exchange rates, and income growth; real measurement is critical for judging investment success.',
      howToInterpret: 'As inflation rises, the same nominal amount buys fewer goods.',
      relatedTerms: ['CPI index', 'Real return', 'Inflation'],
    },
  },
  'spread': {
    en: {
      title: 'Spread',
      shortDescription: 'The difference between the bid and ask price.',
      detailedDescription: 'The spread is a major part of transaction cost and appears in bank FX rates and exchange order books.',
      howToInterpret: 'A wider spread raises transaction cost; a tighter spread usually signals a more liquid market.',
      commonMistake: 'Looking only at the mid price and ignoring spread cost.',
      relatedTerms: ['Liquidity', 'Foreign exchange', 'Effective rate'],
    },
  },
  'volatilite': {
    en: {
      title: 'Volatility',
      shortDescription: 'The degree to which prices fluctuate over time.',
      detailedDescription: 'Volatility carries both risk and opportunity and is a core concept in portfolio and market analysis.',
      howToInterpret: 'High volatility can create upside and downside moves alike, so risk management becomes more important.',
      relatedTerms: ['Risk', 'Candlestick chart'],
    },
  },
  'hisse-senedi': {
    en: {
      title: 'Stock',
      shortDescription: 'A security that represents an ownership share in a company.',
      detailedDescription: 'Shareholders may earn returns through price appreciation and dividends tied to company profits and growth.',
      howToInterpret: 'Stock prices reflect company performance, sector trends, and broader market expectations.',
      relatedTerms: ['BIST', 'P&L', 'Volatility', 'Dividend'],
    },
  },
  'bist': {
    en: {
      title: 'BIST',
      shortDescription: 'The collective name for the equity market and indices traded on Borsa Istanbul.',
      detailedDescription: 'BIST indices summarize the overall direction of the local equity market.',
      howToInterpret: 'BIST indices reflect the broad market trend and should be read separately from individual stock performance.',
      relatedTerms: ['Stock', 'Heat map'],
    },
  },
  'doviz': {
    en: {
      title: 'Foreign exchange',
      shortDescription: 'The value of foreign currencies against the Turkish lira or other currencies.',
      detailedDescription: 'Exchange-rate moves affect import costs, inflation, and portfolio diversification.',
      howToInterpret: 'Currency moves can influence imports, inflation, and how diversified a portfolio is.',
      relatedTerms: ['Effective rate', 'Spread', 'CBRT rate'],
    },
  },
  'efektif-kur': {
    en: {
      title: 'Effective rate',
      shortDescription: 'The transaction rates applied when buying or selling cash foreign currency.',
      detailedDescription: 'These are the actual bid and ask prices used by banks and exchange offices.',
      howToInterpret: 'The gap between effective buy and sell rates appears as the spread.',
      relatedTerms: ['Foreign exchange', 'Spread'],
    },
  },
  'gram-altin': {
    en: {
      title: 'Gram gold',
      shortDescription: 'Gold priced per gram in the Turkish market.',
      detailedDescription: 'A widely used local gold reference for investors; it moves with ounce gold prices and exchange rates.',
      howToInterpret: 'Gram gold prices are influenced by ounce gold and currency moves.',
      relatedTerms: ['Ounce gold', 'Foreign exchange'],
    },
  },
  'ons-altin': {
    en: {
      title: 'Ounce gold',
      shortDescription: 'The international benchmark gold price quoted in troy ounces.',
      detailedDescription: 'The primary global indicator for the gold commodity market.',
      howToInterpret: 'Ounce prices are linked to global risk appetite and the U.S. dollar index.',
      relatedTerms: ['Gram gold', 'Commodity futures'],
    },
  },
  'kripto-varlik': {
    en: {
      title: 'Crypto asset',
      shortDescription: 'Digital assets stored and traded on blockchain networks.',
      detailedDescription: 'Assets such as Bitcoin and Ethereum can carry high volatility and regulatory uncertainty.',
      howToInterpret: 'Crypto assets may show high volatility and regulatory uncertainty.',
      relatedTerms: ['Volatility', 'Risk', 'Stablecoin'],
    },
  },
  'fon': {
    en: {
      title: 'Fund',
      shortDescription: 'A collective investment vehicle that invests in multiple assets.',
      detailedDescription: 'Funds can offer professional management, diversification, and liquidity, but they charge management fees.',
      howToInterpret: 'Fund performance depends on strategy, fees, and market conditions.',
      relatedTerms: ['Stock', 'Risk', 'TEFAS'],
    },
  },
  'eurobond': {
    en: {
      title: 'Eurobond',
      shortDescription: 'A long-term debt security usually issued in a foreign currency.',
      detailedDescription: 'Turkey’s foreign-currency debt instruments are tracked in portfolio and macro analysis.',
      howToInterpret: 'Eurobond return reflects the combined effect of interest, credit risk, and currency moves.',
      relatedTerms: ['Bond', 'Interest', 'ISIN'],
    },
  },
  'tahvil': {
    en: {
      title: 'Bond',
      shortDescription: 'A fixed-income security issued by governments or companies to raise debt.',
      detailedDescription: 'A bond investor lends to the issuer; coupon structure and maturity determine the return profile.',
      howToInterpret: 'Bond price and yield move in opposite directions; when rates rise, prices may fall.',
      relatedTerms: ['Treasury bill', 'Coupon', 'Bond yield'],
    },
  },
  'viop': {
    en: {
      title: 'VİOP',
      shortDescription: 'The derivatives market where futures and options are traded in Turkey.',
      detailedDescription: 'Leveraged products are aimed at professional investors and those with higher risk tolerance.',
      howToInterpret: 'Because of leverage, small price moves can produce large outcomes.',
      commonMistake: 'Treating VİOP products like spot equities with low risk.',
      relatedTerms: ['Risk', 'Volatility', 'Commodity futures'],
    },
  },
  'tufe-endeksi': {
    en: {
      title: 'CPI index',
      shortDescription: 'An index that tracks changes in consumer price levels.',
      detailedDescription: 'It shows how the price level evolves over time and is used in real-return calculations.',
      howToInterpret: 'A rising index means prices are higher, but the index level itself is not the inflation rate.',
      commonMistake: 'Treating the CPI index level as the inflation percentage directly.',
      example: 'The purchasing power of TRY 100,000 in 2022 can be estimated today using CPI changes.',
      relatedTerms: ['CPI monthly rate', 'CPI annual rate', 'Real return', 'Inflation'],
    },
  },
  'tufe-aylik': {
    en: {
      title: 'CPI monthly rate',
      shortDescription: 'The percentage change in the CPI index compared with the previous month.',
      detailedDescription: 'One of the most up-to-date indicators of short-term price pressure.',
      howToInterpret: 'It reflects near-term price pressure; seasonal effects should be considered.',
      relatedTerms: ['CPI index', 'CPI annual rate'],
    },
  },
  'tufe-yillik': {
    en: {
      title: 'CPI annual rate',
      shortDescription: 'The percentage change in the CPI index compared with the same month a year earlier.',
      detailedDescription: 'The most widely used definition of annual inflation.',
      howToInterpret: 'The annual rate summarizes longer-term price trends and shapes policy and market expectations.',
      relatedTerms: ['CPI index', 'Real interest rate', 'Real return'],
    },
  },
  'yi-ufe': {
    en: {
      title: 'PPI',
      shortDescription: 'The producer price index; it measures cost pressure at the production stage.',
      detailedDescription: 'It may feed into CPI with a lag and signals supply-side inflation pressure.',
      howToInterpret: 'Rising PPI can create future CPI pressure; the effect may appear with a delay.',
      relatedTerms: ['CPI index', 'Inflation', 'Wholesale price index'],
    },
  },
  'tuketici-guven': {
    en: {
      title: 'Consumer confidence index',
      shortDescription: 'Measures households’ perception of the economic outlook and spending intentions.',
      detailedDescription: 'Watched as a leading indicator of demand and consumption trends.',
      howToInterpret: 'A rising index may signal stronger spending and demand expectations.',
      relatedTerms: ['Economic confidence index'],
    },
  },
  'ekonomik-guven': {
    en: {
      title: 'Economic confidence index',
      shortDescription: 'A combined measure of consumer and producer confidence in the overall economy.',
      detailedDescription: 'Often read together on macro summary screens.',
      howToInterpret: 'A downward trend may point to weaker demand; an upward trend may suggest recovery expectations.',
      relatedTerms: ['Consumer confidence index'],
    },
  },
  'politika-faizi': {
    en: {
      title: 'Policy rate',
      shortDescription: 'The central bank’s main interest rate used to steer monetary policy.',
      detailedDescription: 'The reference rate set by the CBRT in line with inflation and growth objectives.',
      howToInterpret: 'A rate increase usually targets inflation; a cut generally supports growth.',
      relatedTerms: ['Deposit rate', 'Real interest rate', 'Interest'],
    },
  },
  'mevduat-faizi': {
    en: {
      title: 'Deposit rate',
      shortDescription: 'The interest rate paid on time or demand deposits at banks.',
      detailedDescription: 'The return on lira savings; compared with inflation to calculate real return.',
      howToInterpret: 'If nominal deposit return is below inflation, real return may be negative.',
      relatedTerms: ['Policy rate', 'Real interest rate'],
    },
  },
  'reel-faiz': {
    en: {
      title: 'Real interest rate',
      shortDescription: 'The nominal interest rate minus the inflation rate.',
      detailedDescription: 'Shows the true return on cash and deposits in purchasing-power terms.',
      howToInterpret: 'A negative real rate may mean cash and deposits are losing purchasing power.',
      relatedTerms: ['Policy rate', 'CPI annual rate'],
    },
  },
  'portfoy': {
    en: {
      title: 'Portfolio',
      shortDescription: 'The full set of assets held by an investor.',
      detailedDescription: 'Asset classes are held together according to risk and return goals.',
      howToInterpret: 'Portfolio performance is shaped more by allocation and risk management than by any single asset.',
      relatedTerms: ['Position', 'Asset allocation', 'Diversification'],
    },
  },
  'pozisyon': {
    en: {
      title: 'Position',
      shortDescription: 'The amount or value held in a specific asset within a portfolio.',
      detailedDescription: 'An open position is marked to market based on current prices.',
      howToInterpret: 'Position size directly affects portfolio risk.',
      relatedTerms: ['Cost basis', 'Current value', 'P&L'],
    },
  },
  'maliyet-fiyati': {
    en: {
      title: 'Cost basis',
      shortDescription: 'The average price paid when an asset was added to the portfolio.',
      detailedDescription: 'The reference point for profit and loss calculations.',
      howToInterpret: 'If the current price is above cost, an unrealized gain appears.',
      relatedTerms: ['Current value', 'P&L'],
    },
  },
  'guncel-deger': {
    en: {
      title: 'Current value',
      shortDescription: 'The total value of a position calculated at current market prices.',
      detailedDescription: 'One of the most frequently watched live metrics in portfolio summaries.',
      howToInterpret: 'Current value depends on live market prices and can move throughout the day.',
      relatedTerms: ['Cost basis', 'Nominal return'],
    },
  },
  'nominal-getiri': {
    en: {
      title: 'Nominal return',
      shortDescription: 'Return calculated without adjusting for inflation.',
      detailedDescription: 'Commonly shown on performance cards and should be read together with real return.',
      howToInterpret: 'Nominal return can be positive while real return is negative.',
      relatedTerms: ['Real return', 'CPI index'],
    },
  },
  'reel-getiri': {
    en: {
      title: 'Real return',
      shortDescription: 'Nominal return adjusted for inflation.',
      detailedDescription: 'Shows investment performance in terms of actual purchasing power. Critical in high-inflation periods.',
      howToInterpret: 'If real return is negative, purchasing power may have fallen even when portfolio value rose.',
      commonMistake: 'Treating nominal gains as success without checking inflation.',
      relatedTerms: ['CPI index', 'Nominal return', 'Purchasing power'],
    },
  },
  'pnl': {
    en: {
      title: 'P&L',
      shortDescription: 'Profit & Loss; a performance measure showing gain or loss amounts.',
      detailedDescription: 'Tracked at both position and portfolio level.',
      howToInterpret: 'Realized and unrealized P&L should be read separately.',
      relatedTerms: ['Realized gain/loss', 'Unrealized gain/loss'],
    },
  },
  'gerceklesmis-kz': {
    en: {
      title: 'Realized gain/loss',
      shortDescription: 'Profit or loss that is finalized through a sale or position close.',
      detailedDescription: 'The definite outcome that affects cash flow.',
      howToInterpret: 'Realized results hit cash flow; tax and cost effects should be assessed separately.',
      relatedTerms: ['Unrealized gain/loss', 'P&L'],
    },
  },
  'gerceklesmemis-kz': {
    en: {
      title: 'Unrealized gain/loss',
      shortDescription: 'Temporary profit or loss on open positions based on market prices.',
      detailedDescription: 'It is not final until the position is sold.',
      howToInterpret: 'It updates as prices move and remains provisional until a sale occurs.',
      relatedTerms: ['Realized gain/loss', 'Current value'],
    },
  },
  'varlik-dagilimi': {
    en: {
      title: 'Asset allocation',
      shortDescription: 'Shows the weight of asset classes or instruments in a portfolio.',
      detailedDescription: 'A structural view that shapes risk and return profile.',
      howToInterpret: 'Heavy concentration in one asset raises risk; diversification helps balance exposure.',
      relatedTerms: ['Portfolio', 'Risk', 'Pie chart', 'Diversification'],
    },
  },
  'cizgi-grafik': {
    en: {
      title: 'Line chart',
      shortDescription: 'A chart type that plots value changes over time with a continuous line.',
      detailedDescription: 'The most common visualization for price and performance history.',
      howToInterpret: 'Trend direction and period change are easy to read; avoid focusing on a single point.',
      relatedTerms: ['Area chart', 'Sparkline'],
    },
  },
  'alan-grafigi': {
    en: {
      title: 'Area chart',
      shortDescription: 'A line chart with the area beneath filled, emphasizing volume or cumulative effect.',
      detailedDescription: 'Highlights cumulative impact in portfolio value history.',
      howToInterpret: 'The filled area emphasizes total value or cumulative performance.',
      relatedTerms: ['Line chart'],
    },
  },
  'mum-grafik': {
    en: {
      title: 'Candlestick chart',
      shortDescription: 'A chart showing open, close, high, and low prices for each time interval.',
      detailedDescription: 'The standard display in technical analysis.',
      howToInterpret: 'A single candle is not enough for a decision; read it with trend and timeframe context.',
      commonMistake: 'Inferring long-term direction from one day’s candle.',
      relatedTerms: ['Volatility', 'Trading volume', 'RSI'],
    },
  },
  'isi-haritasi': {
    en: {
      title: 'Heat map',
      shortDescription: 'A visualization that shows asset performance using color and box size.',
      detailedDescription: 'Compares many instruments at a glance in market summaries.',
      howToInterpret: 'Color usually shows return direction; box size may represent weight or market cap.',
      commonMistake: 'Assuming color and box size represent the same metric.',
      relatedTerms: ['BIST', 'Stock', 'Category pulse'],
    },
  },
  'pasta-grafik': {
    en: {
      title: 'Pie chart',
      shortDescription: 'A circular chart that shows how a whole is divided into parts.',
      detailedDescription: 'Used for portfolio asset allocation.',
      howToInterpret: 'Small slices are hard to read; pie charts work best with a few large segments.',
      relatedTerms: ['Asset allocation'],
    },
  },
  'bar-grafik': {
    en: {
      title: 'Bar chart',
      shortDescription: 'Compares values across categories using vertical or horizontal bars.',
      detailedDescription: 'Clear for performance comparisons.',
      howToInterpret: 'Bar length shows magnitude differences clearly; check the axis scale.',
      relatedTerms: ['Performance comparison'],
    },
  },
  'referans-cizgisi': {
    en: {
      title: 'Reference line',
      shortDescription: 'A horizontal or sloped helper line added to a chart for comparison.',
      detailedDescription: 'Added with drawing tools on analysis screens.',
      howToInterpret: 'Used as support/resistance or target levels; interpretation can be subjective.',
      relatedTerms: ['Support and resistance', 'Candlestick chart'],
    },
  },
  'tooltip': {
    en: {
      title: 'Tooltip',
      shortDescription: 'An on-chart data hint that appears when you hover over a point.',
      detailedDescription: 'Provides precise date and price readings.',
      howToInterpret: 'Lets you read exact values such as date, price, and volume at a specific point.',
      relatedTerms: ['Line chart'],
    },
  },
  'enflasyon-karsilastirma': {
    en: {
      title: 'Inflation comparison',
      shortDescription: 'An analysis tool that compares investment return against inflation or CPI.',
      detailedDescription: 'Makes real performance tangible.',
      howToInterpret: 'If return is below inflation, purchasing power has not been preserved.',
      relatedTerms: ['Real return', 'CPI annual rate'],
    },
  },
  'alternatif-yatirim': {
    en: {
      title: 'Alternative investment simulation',
      shortDescription: 'A tool that models how the same amount might perform across different asset classes.',
      detailedDescription: 'Intended for education and scenario analysis.',
      howToInterpret: 'Simulations based on past data do not guarantee future results; review the assumptions.',
      relatedTerms: ['Return', 'Risk', 'Real return'],
    },
  },
  'gecmis-harcama': {
    en: {
      title: 'Past spending simulation',
      shortDescription: 'Calculates what a past expense would be worth today using CPI.',
      detailedDescription: 'Makes purchasing-power loss concrete.',
      howToInterpret: 'The result illustrates loss of purchasing power and is meant for education and planning.',
      relatedTerms: ['CPI index', 'Purchasing power'],
    },
  },
  'makro-durum-ozeti': {
    en: {
      title: 'Macro overview',
      shortDescription: 'A decision-support view that summarizes key macro indicators on one screen.',
      detailedDescription: 'Presents CPI, interest rates, and confidence indices together.',
      howToInterpret: 'Read indicators together; do not over-interpret a single data point.',
      relatedTerms: ['CPI index', 'Policy rate', 'Economic confidence index'],
    },
  },
  'portfoy-reel-getiri-analizi': {
    en: {
      title: 'Portfolio real return analysis',
      shortDescription: 'Shows portfolio performance in purchasing-power terms after inflation adjustment.',
      detailedDescription: 'Central to portfolio review in high-inflation periods.',
      howToInterpret: 'Positive nominal return with negative real return is common in high-inflation environments.',
      relatedTerms: ['Real return', 'Nominal return', 'CPI index'],
    },
  },
  'fiyat-uyarisi': {
    en: {
      title: 'Price alert',
      shortDescription: 'A notification sent when a defined price condition is met.',
      detailedDescription: 'For information only; not investment advice.',
      howToInterpret: 'A triggered alert is not a recommendation; it is informational.',
      relatedTerms: ['Notification preference', 'In-app notification'],
    },
  },
  'portfoy-uyarisi': {
    en: {
      title: 'Portfolio alert',
      shortDescription: 'A notification triggered by portfolio value or position conditions.',
      detailedDescription: 'Configured with personal thresholds.',
      howToInterpret: 'Thresholds should match your personal risk tolerance.',
      relatedTerms: ['Price alert', 'Risk notification'],
    },
  },
  'makro-veri-bildirimi': {
    en: {
      title: 'Macro data notification',
      shortDescription: 'A notification type that informs users about major macro data releases.',
      detailedDescription: 'Used for events such as CPI releases and rate decisions.',
      howToInterpret: 'Compare the release with market expectations when reading it.',
      relatedTerms: ['CPI monthly rate', 'Policy rate'],
    },
  },
  'risk-bildirimi': {
    en: {
      title: 'Risk notification',
      shortDescription: 'An alert sent when unusual volatility or a risk threshold is exceeded.',
      detailedDescription: 'A reminder for risk management.',
      howToInterpret: 'The notification is a risk-management reminder; it does not execute trades.',
      relatedTerms: ['Volatility', 'Risk'],
    },
  },
  'bildirim-tercihi': {
    en: {
      title: 'Notification preference',
      shortDescription: 'Settings that define which channels and topics send notifications.',
      detailedDescription: 'Managed from profile settings.',
      howToInterpret: 'Turning off noise helps focus; keep critical alerts enabled.',
      relatedTerms: ['Email notification', 'In-app notification'],
    },
  },
  'eposta-bildirimi': {
    en: {
      title: 'Email notification',
      shortDescription: 'Delivery of alerts through the email channel.',
      detailedDescription: 'Suitable for longer summary messages.',
      howToInterpret: 'There may be delay; in-app channels may be better for urgent market moves.',
      relatedTerms: ['In-app notification', 'Notification preference'],
    },
  },
  'uygulama-ici-bildirim': {
    en: {
      title: 'In-app notification',
      shortDescription: 'A notification shown instantly in the portal session or mobile app.',
      detailedDescription: 'Listed in the notification center.',
      howToInterpret: 'Unread notifications appear under the bell icon.',
      relatedTerms: ['Price alert', 'Notification preference'],
    },
  },
  'audit-log': {
    en: {
      title: 'Audit log',
      shortDescription: 'A compliance-oriented log of actions performed in the system.',
      detailedDescription: 'An operational trace record for administration and compliance teams.',
      howToInterpret: 'Answers who did what and when.',
      relatedTerms: ['Trace', 'Correlation ID'],
    },
  },
  'trace': {
    en: {
      title: 'Trace',
      shortDescription: 'A distributed trace record that follows a request end to end across services.',
      detailedDescription: 'Part of the observability stack.',
      howToInterpret: 'Used for latency and root-cause analysis of errors.',
      relatedTerms: ['Correlation ID'],
    },
  },
  'correlation-id': {
    en: {
      title: 'Correlation ID',
      shortDescription: 'A unique identifier that links log and trace records for the same user request.',
      detailedDescription: 'Used as a reference number in support and operations workflows.',
      howToInterpret: 'Support and operations teams share this ID when troubleshooting.',
      relatedTerms: ['Trace', 'Audit log'],
    },
  },
}
