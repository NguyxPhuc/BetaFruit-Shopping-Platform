-- SQL Server: legacy ONLINE orders use the application's canonical VNPAY value.
-- Run against the application database after saving the original values/constraint.
-- Does not process payments, change order status, or deduct stock.
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 10000;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @orderCount bigint;
    SELECT @orderCount = COUNT_BIG(*) FROM dbo.[Order] WITH (TABLOCKX, HOLDLOCK);

    IF EXISTS (
        SELECT 1 FROM dbo.[Order]
        WHERE PaymentMethod IS NULL
           OR PaymentMethod COLLATE Latin1_General_100_BIN2 NOT IN ('COD', 'ONLINE', 'Online', 'VNPAY')
    )
        THROW 50001, 'Unexpected PaymentMethod; inspect data before migrating.', 1;

    IF EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.[Order]')
          AND name = N'CHK_Order_Payment'
    )
        ALTER TABLE dbo.[Order] DROP CONSTRAINT CHK_Order_Payment;

    UPDATE dbo.[Order]
       SET PaymentMethod = 'VNPAY'
     WHERE PaymentMethod COLLATE Latin1_General_100_BIN2 IN ('ONLINE', 'Online');

    ALTER TABLE dbo.[Order] WITH CHECK ADD CONSTRAINT CHK_Order_Payment
        CHECK (PaymentMethod COLLATE Latin1_General_100_BIN2 IN ('COD', 'VNPAY'));
    ALTER TABLE dbo.[Order] CHECK CONSTRAINT CHK_Order_Payment;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
