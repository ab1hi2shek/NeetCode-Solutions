# Design a stock exchange

To start, let's define these problem statements:

## Problem statement/Functional requirement
1. Clients/Users should be able to place sell and buy orders to the exchange. 
2. Exchange should match buyers and sellers of the stock.
3. Exchange should provide an ordered list of Asks and Bids (Order book) that everyone can agree to.
    - Maintain real-time order books per stock/symbol.
    - Data structures to insert/cancel/modify quickly.
4. Exchange should provide an ordered list of all activities for a client back to the client when asked.

### Below the line for now
1. Exchange should provide Market Data Feed to clients/brokers.
    - Push live price ticks (top of book, trades, depth)
    - Low latency broadcast to brokers.
2. Trade Settlement: Clearing and settlement after match
3. Risk Checks & Circuit Breakers
    - Prevent market manipulation (price bands, volume caps)
    - Per-user/broker limits

## Basic financial terms 101
### Order book
 - An order book lists the number of shares being bid on or offered at each price point or market depth.
 - Order books are used by almost every exchange for various assets like stocks, bonds, currencies, and even cryptocurrencies.
 - There are three parts to an order book: buy orders, sell orders, and order history.
    - Buy orders contain buyer information including all the bids and the amount they wish to purchase.
    - Sell orders resemble buy orders, but instead include all the offers (or asking prices) or where people are willing to sell.
    - Market order histories show all the transactions that have taken place in the past.
    - The top of the book is where you'll find the highest bid and lowest ask prices.

* We will see one or more trades executed when there exists both bid and ask such that bid price >= ask price.
 - Ask price: The minimum price on which someone is willing to sell a stock (minumum asking price for a asset).
 - Bid price: max price someone wants to pay to buy that asset.

 Ask price can be like $120 x 100 -> meaning this person is willing to sell 100 stocks at $120 or above.

### Other basic terms
- **Symbol:** An abbreviation used to uniquely identify a stock (e.g. META, AAPL). Also known as a "ticker".
- **Order:** An order to buy or sell a stock. Can be a market order or a limit order.
- **Market Order:** An order to trigger immediate purchase or sale of a stock at the current market price. Has no price target and just specifies a number of shares.
- **Limit Order:** An order to purchase or sell a stock at a specified price. Specifies a number of shares and a target price, and can sit on an exchange waiting to be filled or cancelled by the original creator of the order.
