ALMA Registry Service (1.0.1)
---------------------

This project is the base for a future IVOA registry service at ALMA. It could support:

- complete service lookup via RegTAP API
- publishing CADC registry records via OAI-PMH or some other protocol (TBD)

For now, it serves a canned query result (resource-caps):

<resourceID> = <capabilities URL>

and, for data collections, also hosts the capabilities document that describes services
that can be used with a publisherID value from the collection.
