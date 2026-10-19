-- Run in the same SQL Server database used by the application.
-- RoleName.java defines DELIVERY_STAFF. SHIPPER is not an application enum.
-- Rename the existing role in place: preserve RoleId and all UserRole links.
-- No enum, converter, user account, password, or permission link is changed.
-- If a CHECK constraint rejects DELIVERY_STAFF, this transaction rolls back;
-- inspect the constraint with the team instead of disabling it here.

SELECT r.RoleId, r.RoleName, COUNT_BIG(ur.UserId) AS AssignedUserCount
FROM dbo.[Role] AS r
LEFT JOIN dbo.UserRole AS ur ON ur.RoleId = r.RoleId
GROUP BY r.RoleId, r.RoleName;

SELECT name, definition
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.[Role]');

SET XACT_ABORT ON;
SET LOCK_TIMEOUT 10000;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @roleCount bigint;
    SELECT @roleCount = COUNT_BIG(*) FROM dbo.[Role] WITH (TABLOCKX, HOLDLOCK);

    DECLARE @legacyCount bigint;
    SELECT @legacyCount = COUNT_BIG(*)
    FROM dbo.[Role]
    WHERE RoleName COLLATE Latin1_General_100_BIN2 = 'SHIPPER';

    IF @legacyCount > 1
        THROW 50001, 'Multiple SHIPPER roles exist; inspect duplicates before migrating.', 1;

    IF @legacyCount = 1
    BEGIN
        IF EXISTS (
            SELECT 1 FROM dbo.[Role]
            WHERE RoleName COLLATE Latin1_General_100_BIN2 = 'DELIVERY_STAFF'
        )
            THROW 50002, 'Both SHIPPER and DELIVERY_STAFF exist; inspect role assignments before merging.', 1;

        UPDATE dbo.[Role]
        SET RoleName = 'DELIVERY_STAFF'
        WHERE RoleName COLLATE Latin1_General_100_BIN2 = 'SHIPPER';

        IF @@ROWCOUNT <> 1
            THROW 50003, 'Unexpected number of updated roles; migration rolled back.', 1;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- With an already normalized database, rerunning the script updates no rows.
SELECT RoleId, RoleName
FROM dbo.[Role]
WHERE RoleName COLLATE Latin1_General_100_BIN2 IN ('SHIPPER', 'DELIVERY_STAFF');
