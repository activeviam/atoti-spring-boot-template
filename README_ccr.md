# Counterparty Credit Risk Project

This project explores the concept of Counterparty Credit Risk, how to calculate the risk using different methodologies, and how to analyse and view the risk data.

## Outline
We would like to calculate and analyse our counterparty credit risk exposure from OTC derivatives. Derivatives contracts are dynamic in nature, with their current market value - and hence the risk associated with them - changing over time. In the case of a standard instrument like a loan of a mortgage - the risk is known upfront.

Eg for a £20,000 loan, we know that if the counterparty goes bankrupt, our total maximum exposure is £20,000 (in reality it would be less when taking into account what has already been paid off and the possibility of recovering some of the money (eg LGD)).

In the above example, the notional of the trade was £20,000. For derivatives, the notional does not reflect the actual risk, and therefore the calculation methodology is different.

A better approach is to look at the current market value (MV or MtM). This is the current value (and hence exposure) of the trade, which will change continuously as markets move. We can also model how the market value (exposure) will change through time, although we will not cover that here.

The MV can be positive or negative. If it is positive, we are in the money so to speak and stand to profit. If it is negative, we will owe the counterparty money. When calculating our exposure, we typically only want to look at the positive MV - as this is the exposure we have. The negative MV are the exposures our counterparties have against us.

All our trades are subject to a netting agreement with the counterparty - this is typically governed by an ISDA master agreement. However, we might have multiple netting agreements with a single counterparty. Eg say we have 10 trades with a counterparty (Barclays), 3 trades might be under netting_set_barc1, 4 trades under netting_set_barc2, 3 trades for netting_set_barc3 - and we can only net trades within a given netting set.

## Data
We have a set of data for all of our outstanding OTC derivatives trades. The data is at Trade level, and for each Trade we have various attributes and numerical values including:


- TradeId
- NettingSet
- Maturity
- Asset Class
- Product Information
- Notional Value 
- Current Market Value (Mark-to-Market value)


We also have data for the Netting Sets which includes
- Netting Set Name
- CounterpartyId
- Collateral balance

In addition to these two datasets, we also have a wide variety of other reference data such as: counterparty details, book/desk details. Data will eventually be available for each day. Where the date is required, it will be part of the filename.

Finally, as the trade values are not in a common currency, we have daily FX rates.

To start with, you will be given a small sample of ~ 10k trades to develop and prototype with. The realy dataset will contain approx 600k trades per day when it is deployed. You will want to compare data across days.

## Calculations

We propose three methodologies for calculating our credit risk.
Net positive exposure 
Net positive exposure with collateral
Net positive exposure + potential future exposure (PFE) notional addon with collateral

### Methodology 1
```
Counterparty Credit Risk = Net Positive Exposure
```
ie what is our positive exposure 
### Methodology 2
```
Counterparty Credit Risk = Net Positive Exposure - Collateral Held
```
ie what is our net positive exposure after we take into account the collateral held for each netting set
### Methodology 3
```
Counterparty Credit Risk = Net Positive Exposure + PFE addon - Collateral Held
```
ie same as above but with the affect of the PFE addon

In all cases, our exposure cannot be less than 0


### PFE notional addon
The PFE notional addon is calculated by multiplying the notional values of the contracts with a fixed percentage which is based on the PFE Add-on Factor. PFE Add-on Factor is based on the asset class and on the remaining maturity of the contract.


|                   | Interest Rates | FX and Gold | Equities | Precious metals (except Gold) | Other commodities |
|-------------------|----------------|-------------|----------|-------------------------------|-------------------|
| < 1 year          | 0%             | 1%          | 6%       | 7%                            | 10%               |
| 1 year to 5 years | 0.50%          | 5%          | 8%       | 7%                            | 12%               |
| > 5 years         | 1.50%          | 7.50%       | 10%      | 10%                           | 15%               |

Eg: a \$100m IR Swap contract with maturity of 3 years with have a PFE addon of \$100m * 0.005 = \$500,000


## Outputs
You are required to create an Atoti Server cube using the data provided and calculation methodology above. An end user should be able to then:

- View the exposure using different models across counterparties
- View the exposure against different trading books and desks
- View the exposure against different countries of risk

You will need to include Atoti UI in the project for exploring the data. You should also create a set of predefined dashboards to help answer these questions, and present these once the project is complete. Once your work is finished locally, you could think about deploying the application to the cloud so that everyone can access it and view your dashboards.

## Extension Points (AP side)
To be decided if time allows
- Deploy the application to one of the cloud providers (AWS, Azure, Google) and get familiar with deploying AP (eg could be done with a VM, docker etc)
- Load data from other sources, such as data from a database, parquet or kafka queue
- Introduce a real time element - add new trades or change current market value so the calculations can be recomputed as the underlying data changes (could be as simple as dropping in a new file)
- Add Limits module to allow us to set and monitor credit limits against our counterparties/sectors/countries. See the [Limits docs](https://docs.activeviam.com/products/modules/limits/latest/online-help/dev/integration/java.html)
- Add a What-If element in order to perform simulations on collateral amounts of changing of parameters. See the [What-If docs](https://docs.activeviam.com/products/modules/whatif/latest/online-help/dev.html)
- Add further (vector) data in order to calculate the expected exposure in the future

## Extension Points (UI side)
- Create a custom widget to do what-if analysis: change the collateral amounts per netting set and see how that affects the calculations
- Create a custom widget to do what-if analysis: change the weightings in the PFE addon calculation and see how that affects the calculations
- Explore ideas for creating funky dashboards eg a choropleth map chart to show risk per country or region

## Recommended Steps
- Create an empty Atoti Server project
- Define a data model and load the starting data
- Implement a basic cube exposing hierarchies, levels and current measures
- Add Atoti UI to explore the data
- Think about how to aggregate values in different currencies
- Add the required calculations to calculate the credit exposure with the two different methodologies
- Create dashboards in the UI to explore the data
- Deploy application
- Optimise the performance on a larger dataset
- Present application
