# S26CS6.401 — Software Engineering
## Take Home Activity 2: Design Patterns Implementation

**Team Number:** [TO BE ADDED]
**Team Members:** [TO BE ADDED]
**Date:** March 4, 2026

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Assumptions](#2-assumptions)
3. [Design Pattern Analysis](#3-design-pattern-analysis)
   - [3.1 Adapter Pattern (Applied)](#31-adapter-pattern--applied)
   - [3.2 Factory Pattern (Applied)](#32-factory-pattern--applied)
   - [3.3 Strategy Pattern (Applied)](#33-strategy-pattern--applied)
   - [3.4 Chain of Responsibility Pattern (Applied)](#34-chain-of-responsibility-pattern--applied)
   - [3.5 Builder Pattern (Not Applied)](#35-builder-pattern--not-applied)
   - [3.6 Observer Pattern (Not Applied)](#36-observer-pattern--not-applied)
4. [Code Quality Metrics Analysis](#4-code-quality-metrics-analysis)
5. [Conclusion](#5-conclusion)

---

## 1. Introduction

The system under analysis is a **Flight Reservation System** — a Java 11 Maven project that models airports, flights, aircraft, passengers, customers, and booking orders. Its core flow is: a `Customer` creates a `FlightOrder` over a set of `ScheduledFlight` objects and pays using a credit card or PayPal.

Despite passing all 20 unit tests, a structural inspection of the original codebase revealed several code quality problems: untyped object references, scattered creational logic, embedded payment algorithms, duplicated validation logic, and broken encapsulation. Each of these problems maps to a recognized design pattern solution — or, in two cases, reveals that applying a pattern would be inappropriate.

The analysis below evaluates all six patterns mandated by the assignment, applies four of them, and provides principled reasoning for rejecting the remaining two.

---

## 2. Assumptions

The following assumptions govern the design decisions made throughout this analysis:

1. **Aircraft classes treated as closed for modification.** `PassengerPlane`, `Helicopter`, and `PassengerDrone` originate from the upstream repository and are assumed to be third-party or legacy classes whose signatures cannot be changed. This is the primary justification for the Adapter pattern. In practice, getters were added to `PassengerPlane` and `PassengerDrone` to fix broken encapsulation — but the decision to wrap them rather than refactor the rest of the system to use them directly is still an Adapter decision.

2. **Helicopter crew capacity is a domain constant.** Helicopters commercially carry exactly 2 crew members. `HelicopterAdapter` hardcodes `getCrewCapacity()` returning `2`. This is not a magic number — it is a well-known domain fact, and `Helicopter` has no API to expose it.

3. **Aircraft model strings are the authoritative identifiers.** The `Airport.allowedAircrafts` field stores model name strings. The `AircraftFactory` treats these strings as the canonical keys for creation.

4. **The `noFlyList` is a static system-level list.** It is stored as a `static` field in `FlightOrder` and is not intended to be user-configurable at runtime.

5. **Validation throwing `IllegalStateException` is the correct contract.** The original code used boolean returns (`isOrderValid()`). After applying Chain of Responsibility, handlers throw `IllegalStateException` on failure — consistent with existing behaviour in `processOrderWithCreditCard` and `processOrderWithPayPal`.

6. **The scope of this refactoring is structural, not feature-additive.** The goal is to improve the existing code's maintainability and extensibility — not to introduce new runtime behaviour. Patterns considered only in a feature-adding capacity (e.g. Observer for new notification emails) are therefore out of scope.

---

## 3. Design Pattern Analysis

---

### 3.1 Adapter Pattern — Applied

#### Pattern Description

The **Adapter** pattern converts the interface of a class into another interface that clients expect. It lets classes work together that could not otherwise because of incompatible interfaces. An Adapter wraps an existing object (the *adaptee*) and exposes a target interface to the client, translating calls as needed. It is a structural pattern.

#### Problem Identified

The three aircraft classes — `PassengerPlane`, `Helicopter`, and `PassengerDrone` — share no common interface or base class, yet the system needs to treat them uniformly. The consequence is pervasive `instanceof` chains throughout `ScheduledFlight` and `Flight`.

In `Flight.isAircraftValid()`:

```java
// BEFORE — Flight.java
protected Object aircraft;  // untyped

private boolean isAircraftValid(Airport airport) {
    return Arrays.stream(airport.getAllowedAircrafts()).anyMatch(x -> {
        String model;
        if (this.aircraft instanceof PassengerPlane) {
            model = ((PassengerPlane) this.aircraft).model;  // public field access
        } else if (this.aircraft instanceof Helicopter) {
            model = ((Helicopter) this.aircraft).getModel();
        } else if (this.aircraft instanceof PassengerDrone) {
            model = "HypaHype";  // hardcoded model name
        } else {
            throw new IllegalArgumentException("Aircraft is not recognized");
        }
        return x.equals(model);
    });
}
```

In `ScheduledFlight.getCapacity()` and `getCrewMemberCapacity()`:

```java
// BEFORE — ScheduledFlight.java
public int getCapacity() throws NoSuchFieldException {
    if (this.aircraft instanceof PassengerPlane) {
        return ((PassengerPlane) this.aircraft).passengerCapacity;  // public field
    }
    if (this.aircraft instanceof Helicopter) {
        return ((Helicopter) this.aircraft).getPassengerCapacity();
    }
    if (this.aircraft instanceof PassengerDrone) {
        return 4;  // magic number — capacity not in the class at all
    }
    throw new NoSuchFieldException("this aircraft has no information about its capacity");
}

public int getCrewMemberCapacity() throws NoSuchFieldException {
    if (this.aircraft instanceof PassengerPlane) {
        return ((PassengerPlane) this.aircraft).crewCapacity;  // public field
    }
    if (this.aircraft instanceof Helicopter) {
        return 2;  // hardcoded
    }
    if (this.aircraft instanceof PassengerDrone) {
        return 0;  // hardcoded
    }
    throw new NoSuchFieldException("this aircraft has no information about its crew capacity");
}
```

Problems with this code:
- `aircraft` typed as `Object` — zero compile-time type safety; any object can be passed
- Every new aircraft type requires modifying `Flight`, `ScheduledFlight`, and every method that touches aircraft — violates the **Open/Closed Principle**
- `PassengerPlane` exposes `passengerCapacity` and `crewCapacity` as `public` fields — broken encapsulation
- `PassengerDrone` has no capacity fields at all — data is scattered as magic numbers in `ScheduledFlight`
- `NoSuchFieldException` is a checked exception being used to signal missing data — incorrect semantics

#### Solution Applied

A target `Aircraft` interface was defined. Three adapter classes wrap the existing aircraft, translating their disparate APIs to the common interface. `PassengerPlane` and `PassengerDrone` were also fixed to use private fields and proper getters (encapsulation fix that makes the adapters meaningful).

**Aircraft interface (new):**

```java
// AFTER — Aircraft.java
public interface Aircraft {
    String getModel();
    int getPassengerCapacity();
    int getCrewCapacity();
}
```

**PassengerPlaneAdapter (new):**

```java
// AFTER — PassengerPlaneAdapter.java
public class PassengerPlaneAdapter implements Aircraft {
    private final PassengerPlane plane;

    public PassengerPlaneAdapter(PassengerPlane plane) {
        this.plane = plane;
    }

    @Override public String getModel()            { return plane.getModel(); }
    @Override public int getPassengerCapacity()   { return plane.getPassengerCapacity(); }
    @Override public int getCrewCapacity()        { return plane.getCrewCapacity(); }
}
```

**Flight and ScheduledFlight after:**

```java
// AFTER — Flight.java
protected Aircraft aircraft;  // typed

private boolean isAircraftValid(Airport airport) {
    String model = this.aircraft.getModel();  // single polymorphic call
    return Arrays.stream(airport.getAllowedAircrafts()).anyMatch(x -> x.equals(model));
}
```

```java
// AFTER — ScheduledFlight.java
public int getCapacity() {                     // NoSuchFieldException gone
    return this.aircraft.getPassengerCapacity();
}

public int getCrewMemberCapacity() {
    return this.aircraft.getCrewCapacity();
}
```

#### Class Diagrams

**Before:**

![Adapter Before](Class%20Diagrams/Adapter%20Pattern/adapter-before.pdf)

**After:**

![Adapter After](Class%20Diagrams/Adapter%20Pattern/adapter-after.pdf)

#### Benefits

- **Type safety restored.** `aircraft` is now `Aircraft` — invalid types are caught at compile time.
- **Open/Closed Principle satisfied.** Adding a new aircraft type (e.g. a `Seaplane`) requires only a new `SeaplaneAdapter` and a new `AircraftFactory` case — `Flight` and `ScheduledFlight` are untouched.
- **`instanceof` chains eliminated.** Both `isAircraftValid()` and `getCapacity()`/`getCrewMemberCapacity()` are now single-line polymorphic calls.
- **Checked exception removed.** `NoSuchFieldException` was an inappropriate use of a checked exception for missing data — the interface contract guarantees the data exists.
- **Encapsulation improved.** `PassengerPlane`'s public fields and `PassengerDrone`'s absent capacity fields are fixed behind proper getters.

#### Drawbacks

- **Class count increases.** Three adapter classes are added for three aircraft types. In a system with many aircraft types, this could grow.
- **Indirection layer added.** Every call to an aircraft now routes through an adapter, adding one level of delegation. In this codebase the performance impact is negligible.
- **HelicopterAdapter hardcodes crew capacity.** Because `Helicopter` exposes no `getCrewCapacity()`, the adapter returns `2` — a domain constant. This is acceptable but would need updating if the domain model changes.

---

### 3.2 Factory Pattern — Applied

#### Pattern Description

The **Factory Method** (and its simpler variant, **Static Factory**) pattern defines a single creation point for objects without specifying their concrete classes at the call site. Clients request an object by some identifier; the factory decides which concrete class to instantiate and returns it typed as an abstraction. It is a creational pattern that centralises and encapsulates object construction.

#### Problem Identified

Aircraft objects were instantiated directly by callers — `Runner.java` and both test files — using `new PassengerPlane(...)`, `new Helicopter(...)`, `new PassengerDrone(...)`:

```java
// BEFORE — Runner.java
static List<Object> aircrafts = Arrays.asList(
    new PassengerPlane("A380"),
    new PassengerPlane("A350"),
    new PassengerPlane("Embraer 190"),
    new PassengerPlane("Antonov AN2"),
    new Helicopter("H1"),
    new PassengerDrone("HypaHype")
);
```

The same pattern was duplicated in `ScheduleTest.java` and `ScenarioTest.java`. Problems:

- **Creation logic is scattered** across three files. If a new aircraft type is added, or if the wrapping strategy changes, every caller must be updated.
- **The list type is `Object`** — callers have no shared abstraction to program against.
- **Model string validation is absent** — passing an unrecognised model silently creates an object with an unknown model, which only fails at runtime (during `Airport.getAllowedAircrafts()` matching).
- **The concrete classes are exposed** to every caller — tight coupling between callers and the concrete aircraft hierarchy.

#### Solution Applied

A static factory class `AircraftFactory` was introduced. All callers were updated to use `AircraftFactory.create(model)` and type their collections as `List<Aircraft>`.

```java
// AFTER — AircraftFactory.java
public class AircraftFactory {
    public static Aircraft create(String model) {
        switch (model) {
            case "A380":
            case "A350":
            case "Embraer 190":
            case "Antonov AN2":
                return new PassengerPlaneAdapter(new PassengerPlane(model));
            case "H1":
            case "H2":
                return new HelicopterAdapter(new Helicopter(model));
            case "HypaHype":
                return new PassengerDroneAdapter(new PassengerDrone(model));
            default:
                throw new IllegalArgumentException(
                    String.format("Aircraft model '%s' is not recognized", model));
        }
    }
}
```

```java
// AFTER — Runner.java
static List<Aircraft> aircrafts = Arrays.asList(
    AircraftFactory.create("A380"),
    AircraftFactory.create("A350"),
    AircraftFactory.create("Embraer 190"),
    AircraftFactory.create("Antonov AN2"),
    AircraftFactory.create("H1"),
    AircraftFactory.create("HypaHype")
);
```

#### Class Diagrams

**Before:**

![Factory Before](Class%20Diagrams/Factory%20Pattern/factory-before.pdf)

**After:**

![Factory After](Class%20Diagrams/Factory%20Pattern/factory-after.pdf)

#### Benefits

- **Single creation point.** Adding a new aircraft model requires changing only `AircraftFactory` — callers are unaffected.
- **Early validation.** An unrecognised model string throws `IllegalArgumentException` at construction time rather than silently producing a broken object.
- **Callers decouple from concrete classes.** `Runner`, `ScheduleTest`, and `ScenarioTest` no longer import `PassengerPlane`, `Helicopter`, or `PassengerDrone` — they only see `Aircraft`.
- **Works synergistically with the Adapter.** The factory is the only place where adapters are wired to adaptees — the rest of the system never sees adapter classes either.

#### Drawbacks

- **Model strings as keys are stringly typed.** Using `"A380"` as an identifier rather than an enum or constant means a typo in a caller compiles but throws at runtime. An enum-based factory would close this gap.
- **`switch` statement grows with each new type.** This is a well-known limitation of the Static Factory variant — a registry or map-based factory would scale better.

---

### 3.3 Strategy Pattern — Applied

#### Pattern Description

The **Strategy** pattern defines a family of algorithms, encapsulates each one in a separate class, and makes them interchangeable. The context holds a reference to a strategy interface and delegates the work to it, without knowing which concrete strategy is in use. It is a behavioural pattern that replaces conditional branching over algorithm variants with polymorphism.

#### Problem Identified

`FlightOrder` directly implemented two complete payment processing pipelines inside itself:

```java
// BEFORE — FlightOrder.java (selected methods)
public boolean processOrderWithCreditCard(CreditCard creditCard) throws IllegalStateException {
    if (isClosed()) return true;
    if (!cardIsPresentAndValid(creditCard)) {
        throw new IllegalStateException("Payment information is not set or not valid.");
    }
    boolean isPaid = payWithCreditCard(creditCard, this.getPrice());
    if (isPaid) this.setClosed();
    return isPaid;
}

public boolean payWithCreditCard(CreditCard card, double amount) throws IllegalStateException {
    if (cardIsPresentAndValid(card)) {
        double remainingAmount = card.getAmount() - getPrice();
        if (remainingAmount < 0) throw new IllegalStateException("Card limit reached");
        card.setAmount(remainingAmount);
        return true;
    }
    return false;
}

public boolean processOrderWithPayPal(String email, String password) throws IllegalStateException {
    if (isClosed()) return true;
    if (email == null || password == null || !email.equals(Paypal.DATA_BASE.get(password))) {
        throw new IllegalStateException("Payment information is not set or not valid.");
    }
    boolean isPaid = payWithPayPal(email, password, this.getPrice());
    if (isPaid) this.setClosed();
    return isPaid;
}

public boolean payWithPayPal(String email, String password, double amount) {
    if (email.equals(Paypal.DATA_BASE.get(password))) {
        System.out.println("Paying " + getPrice() + " using PayPal.");
        return true;
    }
    return false;
}

private boolean cardIsPresentAndValid(CreditCard card) {
    return card != null && card.isValid();
}
```

Problems with this code:
- **`FlightOrder` has six payment-related methods** — far beyond its core responsibility of representing an order.
- **Adding a new payment method** (e.g. Apple Pay, bank transfer) requires modifying `FlightOrder` — violates the **Open/Closed Principle**.
- **`FlightOrder` is tightly coupled** to both `CreditCard` and `Paypal` — two concrete payment infrastructure classes.
- **Validation and execution are interleaved** inside the same class that manages order state — violates the **Single Responsibility Principle**.

#### Solution Applied

A `PaymentStrategy` interface was defined. `CreditCardPayment` and `PayPalPayment` implement it. Validation logic moved into each strategy's constructor (fail-fast at construction time rather than inside `FlightOrder`). `FlightOrder` is reduced to a single delegation call.

```java
// AFTER — PaymentStrategy.java
public interface PaymentStrategy {
    boolean pay(double amount);
}
```

```java
// AFTER — CreditCardPayment.java
public class CreditCardPayment implements PaymentStrategy {
    private final CreditCard card;

    public CreditCardPayment(CreditCard card) {
        if (card == null || !card.isValid()) {
            throw new IllegalStateException("Payment information is not set or not valid.");
        }
        this.card = card;
    }

    @Override
    public boolean pay(double amount) {
        double remaining = card.getAmount() - amount;
        if (remaining < 0) {
            throw new IllegalStateException("Card limit reached");
        }
        System.out.println("Paying " + amount + " using Credit Card.");
        card.setAmount(remaining);
        return true;
    }
}
```

```java
// AFTER — PayPalPayment.java
public class PayPalPayment implements PaymentStrategy {
    private final String email;
    private final String password;

    public PayPalPayment(String email, String password) {
        if (email == null || password == null
                || !email.equals(Paypal.DATA_BASE.get(password))) {
            throw new IllegalStateException("Payment information is not set or not valid.");
        }
        this.email = email;
        this.password = password;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paying " + amount + " using PayPal.");
        return true;
    }
}
```

```java
// AFTER — FlightOrder.java
public boolean processOrder(PaymentStrategy paymentStrategy) throws IllegalStateException {
    if (isClosed()) return true;
    boolean isPaid = paymentStrategy.pay(this.getPrice());
    if (isPaid) this.setClosed();
    return isPaid;
}
```

Callers:

```java
// AFTER — ScenarioTest.java (example usage)
// Credit Card
order.processOrder(new CreditCardPayment(card));

// PayPal
order.processOrder(new PayPalPayment("bob@example.com", "secret"));
```

#### Class Diagrams

**Before:**

![Strategy Before](Class%20Diagrams/Strategy%20Pattern/strategy-before.pdf)

**After:**

![Strategy After](Class%20Diagrams/Strategy%20Pattern/strategy-after.pdf)

#### Benefits

- **OCP satisfied.** Adding a new payment method (e.g. `ApplePayPayment`) requires only creating a new class — `FlightOrder` is untouched.
- **SRP satisfied.** `FlightOrder` manages order lifecycle; strategies manage payment mechanics. Neither knows about the other's internals.
- **Fail-fast validation.** Strategies validate credentials in their constructors — an invalid strategy cannot be passed to `processOrder`.
- **Testability improves.** Payment behaviour can be tested in isolation; `FlightOrder` tests can use a mock strategy.
- **`FlightOrder` reduced from ~60 lines of payment code to a single 5-line method.**

#### Drawbacks

- **More classes.** Two additional classes per payment method. For two methods, this is clearly justified. If the system has 10 payment methods, 10 classes may feel like a lot — though each remains small and focused.
- **Caller must construct the strategy.** The caller now needs to know which concrete strategy to instantiate. In this codebase that is acceptable (callers already knew which payment method to use). In a more complex system, a strategy factory could hide this.

---

### 3.4 Chain of Responsibility Pattern — Applied

#### Pattern Description

The **Chain of Responsibility** pattern passes a request along a chain of handlers. Each handler decides whether to process the request or pass it to the next handler in the chain. It decouples the sender of a request from its receivers and allows multiple objects to handle the request, each handling its own concern independently. It is a behavioural pattern.

#### Problem Identified

Order validation logic was **duplicated** across two separate classes. `Customer.isOrderValid()` and `FlightOrder.isOrderValid()` contained the same three checks:

```java
// BEFORE — Customer.java
private boolean isOrderValid(List<String> passengerNames, List<ScheduledFlight> flights) {
    boolean valid = true;
    valid = valid && !FlightOrder.getNoFlyList().contains(this.getName());
    valid = valid && passengerNames.stream()
                         .noneMatch(p -> FlightOrder.getNoFlyList().contains(p));
    valid = valid && flights.stream().allMatch(sf -> {
        try {
            return sf.getAvailableCapacity() >= passengerNames.size();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        }
    });
    return valid;
}
```

```java
// BEFORE — FlightOrder.java (identical logic, different scope)
private boolean isOrderValid(Customer customer, List<String> passengerNames,
                             List<ScheduledFlight> flights) {
    boolean valid = true;
    valid = valid && !noFlyList.contains(customer.getName());
    valid = valid && passengerNames.stream()
                         .noneMatch(p -> noFlyList.contains(p));
    valid = valid && flights.stream().allMatch(sf -> {
        try {
            return sf.getAvailableCapacity() >= passengerNames.size();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        }
    });
    return valid;
}
```

Problems:
- **Duplicated validation logic.** The same three checks exist in two places. A bug fix or new rule must be applied twice — a classic maintenance hazard.
- **Unclear ownership.** Should validation live in `Customer` or `FlightOrder`? Both claim it — neither is the true owner.
- **Monolithic validation.** A single boolean chain collapses all rules. When it returns `false`, the caller cannot know which rule failed.
- **NoSuchFieldException swallowed silently** — if capacity cannot be retrieved, validation quietly fails.

#### Solution Applied

An abstract `OrderValidationHandler` was defined. Three concrete handlers each own one check. `Customer.createOrder()` builds the chain and runs it. `FlightOrder` no longer performs validation at all.

```java
// AFTER — OrderValidationHandler.java
public abstract class OrderValidationHandler {
    private OrderValidationHandler next;

    public OrderValidationHandler setNext(OrderValidationHandler next) {
        this.next = next;
        return next;  // enables fluent chaining
    }

    public abstract boolean handle(Customer customer,
                                   List<String> passengerNames,
                                   List<ScheduledFlight> flights);

    protected boolean handleNext(Customer customer,
                                 List<String> passengerNames,
                                 List<ScheduledFlight> flights) {
        if (next == null) return true;
        return next.handle(customer, passengerNames, flights);
    }
}
```

```java
// AFTER — CustomerNoFlyHandler.java
public class CustomerNoFlyHandler extends OrderValidationHandler {
    @Override
    public boolean handle(Customer customer, List<String> passengerNames,
                          List<ScheduledFlight> flights) {
        if (FlightOrder.getNoFlyList().contains(customer.getName())) {
            throw new IllegalStateException("Customer is on the no-fly list");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
```

```java
// AFTER — PassengerNoFlyHandler.java
public class PassengerNoFlyHandler extends OrderValidationHandler {
    @Override
    public boolean handle(Customer customer, List<String> passengerNames,
                          List<ScheduledFlight> flights) {
        if (passengerNames.stream().anyMatch(p -> FlightOrder.getNoFlyList().contains(p))) {
            throw new IllegalStateException("A passenger is on the no-fly list");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
```

```java
// AFTER — FlightCapacityHandler.java
public class FlightCapacityHandler extends OrderValidationHandler {
    @Override
    public boolean handle(Customer customer, List<String> passengerNames,
                          List<ScheduledFlight> flights) {
        boolean hasCapacity = flights.stream()
            .allMatch(sf -> sf.getAvailableCapacity() >= passengerNames.size());
        if (!hasCapacity) {
            throw new IllegalStateException("Not enough capacity on one or more flights");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
```

```java
// AFTER — Customer.createOrder()
public FlightOrder createOrder(List<String> passengerNames,
                               List<ScheduledFlight> flights, double price) {
    OrderValidationHandler chain = new CustomerNoFlyHandler();
    chain.setNext(new PassengerNoFlyHandler())
         .setNext(new FlightCapacityHandler());
    chain.handle(this, passengerNames, flights);  // throws on failure

    FlightOrder order = new FlightOrder(flights);
    order.setCustomer(this);
    order.setPrice(price);
    List<Passenger> passengers = passengerNames.stream()
        .map(Passenger::new).collect(Collectors.toList());
    order.setPassengers(passengers);
    order.getScheduledFlights().forEach(sf -> sf.addPassengers(passengers));
    orders.add(order);
    return order;
}
```

#### Class Diagrams

**Before:**

![CoR Before](Class%20Diagrams/Chain%20of%20Responsibility%20Pattern/cor-before.pdf)

**After:**

![CoR After](Class%20Diagrams/Chain%20of%20Responsibility%20Pattern/cor-after.pdf)

#### Benefits

- **Duplication eliminated.** Each validation rule lives in exactly one handler class.
- **Single owner of validation.** `Customer.createOrder()` owns the chain; `FlightOrder` no longer performs validation — clear separation of concerns.
- **Each rule is independently testable.** `CustomerNoFlyHandler`, `PassengerNoFlyHandler`, and `FlightCapacityHandler` can each be unit tested in isolation.
- **Specific failure messages.** Instead of `"Order is not valid"`, each handler throws a message identifying which rule failed — better for debugging and for future error handling.
- **`NoSuchFieldException` eliminated.** Because the Adapter pattern guarantees `getAvailableCapacity()` never throws, the capacity handler no longer needs exception handling.
- **Extensible.** A new validation rule (e.g. blacklisted routes, minimum age) is a new handler class added to the chain — existing handlers untouched.

#### Trade-off Acknowledgement

For three fixed validation rules, Chain of Responsibility introduces four classes (`OrderValidationHandler` + 3 concrete handlers) where a simpler `OrderValidator` utility with three static methods would be fewer moving parts. The pattern is justified here because: (a) the rules were duplicated across two classes with no clear owner; (b) the throw-on-failure contract makes each handler genuinely independent; (c) future rules (route restrictions, age checks, booking quotas) are plausible in a reservation system. If the validation rules were truly fixed forever, a simple utility class would be the simpler choice.

#### Drawbacks

- **Higher class count** for a small number of fixed rules.
- **Chain construction is manual** — each caller must wire the chain. In this system only `Customer.createOrder()` does this, so the cost is contained.
- **Ordering of handlers is implicit** — the wrong ordering (e.g. checking capacity before checking the no-fly list) would not be caught at compile time.

---

### 3.5 Builder Pattern — Not Applied

#### Pattern Description

The **Builder** pattern separates the construction of a complex object from its representation. It provides a step-by-step construction API where each step sets one aspect of the object, and a final `build()` call assembles the result. It is most valuable when an object has many optional and mandatory parameters, or when the same construction process must produce different representations.

#### Why It Was Considered

`FlightOrder` is constructed across multiple steps — `new FlightOrder(flights)`, then `setCustomer()`, `setPrice()`, `setPassengers()`. This multi-step pattern superficially resembles a Builder use case.

#### Why It Was Rejected

1. **Construction happens in exactly one place.** The entire construction sequence sits in `Customer.createOrder()` — a single, private, controlled method. There is no risk of callers assembling an incomplete object, which is the primary problem Builder solves.

2. **No optional vs. mandatory complexity.** All fields (`customer`, `price`, `passengers`, `flights`) are always set together. There is no combinatorial explosion of parameters. A telescoping constructor problem does not exist.

3. **The steps are always the same.** `createOrder()` always calls the same sequence of setters in the same order. A Builder adds value when the same object type needs to be constructed in different ways — that is not the case here.

4. **Builder would add 2–3 classes for zero structural gain.** A `FlightOrderBuilder` with `withCustomer()`, `withPrice()`, `withPassengers()`, `withFlights()`, `build()` would be more verbose than the current four-line construction sequence, with no improvement in safety, readability, or extensibility.

**Verdict: Applying Builder here would be overengineering. The pattern is a solution to a problem this codebase does not have.**

---

### 3.6 Observer Pattern — Not Applied

#### Pattern Description

The **Observer** pattern defines a one-to-many dependency between objects. When a subject (observable) changes state, all registered observers are notified and updated automatically. It is a behavioural pattern that decouples the subject from its observers, enabling event-driven architectures.

#### Why It Was Considered

Booking events — an order being placed, a flight reaching capacity — are natural candidates for notifications. In a production reservation system, one might want to send confirmation emails, update inventory dashboards, or trigger loyalty point calculations when a booking is confirmed.

#### Why It Was Rejected

1. **No existing notification mechanism exists in the codebase.** The original code contains no notification calls, no event system, no subscriber list. Observer would not improve any existing structure — it would add entirely new behaviour. This is a feature addition, not a structural improvement.

2. **The assignment scope is structural refactoring.** The problem statement asks to "apply design patterns to enhance functionality" in the context of the existing code's challenges. Adding an Observer to deliver booking confirmation emails is inventing a new requirement, not addressing a structural flaw.

3. **There is no concrete consumer of booking events.** Observer is valuable when there are actual observers that need decoupling from the subject. Introducing a Subject interface and observer list for zero concrete observers produces dead code.

4. **Adding Observer to `FlightOrder` would couple it to new concerns** (email services, inventory systems) that are entirely absent from the current domain model — the opposite of the isolation benefit Observer provides when used correctly.

**Verdict: Observer is out of scope. It is not that this pattern does not fit the codebase — it is that no problem in the codebase calls for it. Applying it would be adding a feature rather than improving structure.**

---

## 4. Code Quality Metrics Analysis

> **[TO BE ADDED]** — Static analysis tools to be run on `Reservation-System-Starter-Old` and `Reservation-System-Starter-Refactored`. Results, comparison table, and per-metric written analysis will be inserted here.

### 4.1 Tools Used

> **[TO BE ADDED]**

### 4.2 Metrics: Before vs. After

> **[TO BE ADDED]** — Side-by-side comparison table.

### 4.3 Analysis

> **[TO BE ADDED]** — Per-metric analysis (improved / worsened / unchanged, with reasoning).

---

## 5. Conclusion

Four of the six candidate design patterns were identified as applicable and implemented:

| Pattern | Problem Solved | Key Benefit |
|---|---|---|
| Adapter | Incompatible aircraft interfaces, `instanceof` chains | Compile-time type safety, OCP on aircraft types |
| Factory | Scattered aircraft instantiation, no creation contract | Single creation point, model validation, caller decoupling |
| Strategy | Payment logic embedded in `FlightOrder`, OCP violation | Payment methods extensible without touching `FlightOrder` |
| Chain of Responsibility | Duplicated validation across `Customer` and `FlightOrder` | Single owner per rule, independent testability |

Two patterns were evaluated and consciously rejected:

| Pattern | Reason Rejected |
|---|---|
| Builder | No complex construction problem exists; would be overengineering |
| Observer | No existing notification mechanism; application would be feature-addition, not structural improvement |

The net effect of the four applied patterns is a codebase where: (a) new aircraft types, payment methods, and validation rules can each be added by writing new classes without modifying existing ones; (b) type safety is enforced at compile time throughout; (c) checked exceptions arising from absent data are eliminated; and (d) responsibilities are clearly assigned to the class that logically owns them.
