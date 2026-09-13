
- document persisting after submitting to acquiring bank

# Requirements



# Solution: Tim Storer

Implementation notes:
- Idempotency: in a real payment system, we would want to use a client-generated idempotency token
to prevent accidental duplicates being created.  This is ignored here.
- In a real payment system, we would want to store the payment (with "Pending" status) after
validating and before sending the request to the acquiring bank.  This would allow us to cope with
failures, timeouts, etc.  Again, this is ignored for this exercise.
- Testing: there is very little logic, and so almost all the tests are on the controller-level.
